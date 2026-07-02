# Finding CI/CD 配置指南 — GitHub SSH + crontab 自动部署

> `git push` → 服务器每 5 分钟自动检查 → 有更新就增量构建 → 零停机发布

---

## 工作原理

```
┌─────────────────────────────────────────────┐
│  你在 Windows 写代码                           │
│  git push origin master                      │
└──────────────────┬──────────────────────────┘
                   │ SSH (22 端口，国内直连无压力)
                   ▼
┌─────────────────────────────────────────────┐
│  GitHub: daodaoq/finding                     │
└──────────────────┬──────────────────────────┘
                   │
     ╔═════════════╧═════════════╗
     ║  crontab 每 5 分钟触发     ║
     ║  bash cicd/deploy.sh      ║
     ╚═════════════╤═════════════╝
                   │
                   ▼
┌─────────────────────────────────────────────┐
│  deploy.sh 增量构建                            │
│  ① git fetch → 比较有无新提交                   │
│  ② 无新提交 → 直接跳过，3 秒结束                 │
│  ③ 有更新 → git pull → 只编译变更的模块         │
│  ④ 重启后端 + reload Nginx（3~60 秒）           │
└─────────────────────────────────────────────┘
```

**为什么用 crontab 而不是 Webhook**：GitHub 在国内访问偶尔不稳定，Webhook 推送可能超时。SSH 协议（22 端口）走的是不同通道，`git fetch/pull` 很稳定。5 分钟延迟对个人项目完全没影响。

---

## 1. 服务器配置（一次配置，永久自动）

### 1.1 生成 SSH Key

```bash
ssh root@你的服务器

ssh-keygen -t ed25519 -C "finding-server"
# 一路回车，不设密码
```

### 1.2 添加公钥到 GitHub

```bash
cat ~/.ssh/id_ed25519.pub
# 复制输出内容
```

打开 https://github.com/settings/keys → **New SSH Key**：
- Title: `finding-server`
- Key: 粘贴刚才复制的公钥

### 1.3 验证连接

```bash
ssh -T git@github.com
# 看到 "Hi daodaoq!" 说明成功
```

### 1.4 克隆项目到服务器

```bash
cd /root
git clone git@github.com:daodaoq/finding.git
```

### 1.5 创建日志目录 + 赋予执行权限

```bash
mkdir -p /root/finding/deploy/cicd/logs
chmod +x /root/finding/deploy/cicd/deploy.sh
```

### 1.6 添加定时任务

```bash
crontab -e
```

添加这一行：

```
*/5 * * * * bash /root/finding/deploy/cicd/deploy.sh >> /root/finding/deploy/cicd/logs/cron.log 2>&1
```

### 1.7 验证

```bash
# 手动跑一次，确认能正常工作
bash /root/finding/deploy/cicd/deploy.sh
```

看到 `=== 部署完成 ===` 就 OK。

---

## 2. 日常使用

```bash
# Windows 本地写代码 → 推送
git add .
git commit -m "feat: 新功能"
git push origin master

# 搞定。5 分钟内服务器自动完成：
# ① git fetch 发现新提交
# ② 增量编译（只编译改了的模块）
# ③ 重启后端 + 重载 Nginx
```

查看部署日志：

```bash
ssh root@服务器

# CI/CD 调度日志
tail -f /root/finding/deploy/cicd/logs/cron.log

# 后端日志
tail -f /root/finding/deploy/app.log
```

---

## 3. 增量编译策略

`cicd/deploy.sh` 根据变更文件决定编译哪些模块：

| 你改了 | 触发动作 | 耗时 |
|--------|---------|------|
| `finding-web/**` | 构建学生端 + reload nginx | ~10 秒 |
| `finding-admin/**` | 构建管理端 + reload nginx | ~8 秒 |
| `finding-server/**` | 编译 jar + 重启后端 | ~30 秒 |
| 三个都改了 | 全部重新构建 | ~1 分钟 |
| 都没改 | 直接跳过，无操作 | ~1 秒 |

> 只改前端 → 后端不重启，用户无感知。只改后端 → 前端不动，NGINX 不重载。

---

## 4. 手动操作

```bash
# 立即部署（不等 crontab）
bash /root/finding/deploy/cicd/deploy.sh

# 查看最近部署记录
tail -50 /root/finding/deploy/cicd/logs/cron.log

# 修改检查间隔（比如改成 2 分钟）
crontab -e
# */5 改为 */2
```

---

## 5. 故障排查

### git fetch 报 Permission denied
```bash
# 确认 SSH key 已添加到 GitHub
ssh -T git@github.com
# 如果失败：
cat ~/.ssh/id_ed25519.pub   # 确认公钥已复制到 GitHub Settings → SSH Keys
```

### crontab 不执行
```bash
# 检查 crontab 是否在跑
systemctl status cron    # Debian/Ubuntu
systemctl status crond   # CentOS

# 手动测试
bash /root/finding/deploy/cicd/deploy.sh
```

### 编译失败
```bash
tail -50 /root/finding/deploy/cicd/logs/cron.log
# 看具体报错，常见原因：
# - npm ci 失败 → 服务器磁盘满或网络问题
# - mvn package 失败 → Java 版本不对
```

### 想看实时部署状态
```bash
# SSH 登录服务器，开两个窗口：
# 窗口 1: 实时看 CI 日志
tail -f /root/finding/deploy/cicd/logs/cron.log

# 窗口 2: 实时看后端日志
tail -f /root/finding/deploy/app.log
```

---

## 附：push 同时到 GitHub 和 Gitee（备用）

你的 Windows 本地已经配了两个 remote，一个命令同时推两个：

```bash
# 推 GitHub
git push origin master

# 也推 Gitee（当备份）
git push gitee master

# 或者一次推两个
git push origin master && git push gitee master
```

Gitee 留着当镜像备份，万一 GitHub SSH 抽风也能从 Gitee 拉。
