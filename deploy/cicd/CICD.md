# Finding CI/CD 配置指南 — Gitee + Webhook 自动部署

> 推送代码到 Gitee → 服务器自动拉取 → 增量构建 → 不停机发布

---

## 方案对比

| 方案 | 复杂度 | 费用 | 适合场景 |
|------|--------|------|---------|
| **Gitee Webhook + 自建监听** ★ | ⭐ 简单 | 免费 | 个人项目、单服务器 |
| Gitee Go (官方流水线) | ⭐⭐ 中等 | 免费(限高校) | 高校认证用户 |
| Gitee + Jenkins | ⭐⭐⭐ 复杂 | 免费 | 团队项目、多环境 |
| GitHub Actions + 国内镜像 | ⭐⭐ 中等 | 免费 | 有 GitHub 账号 |

> **推荐方案**：Webhook → 服务器自动部署（本文档方案），零依赖，十分钟搭完。

---

## 1. 工作原理

```
你在 Windows 写代码
    │ git push
    ▼
┌─────────────────┐
│  Gitee 远程仓库   │
│  gitee.com/xxx   │
└────────┬────────┘
         │ POST /webhook (携带提交信息)
         ▼
┌─────────────────┐
│  你的 Linux 服务器  │
│  Node.js Webhook  │  ← PM2 守护，端口 9000
│  验证签名 → 触发   │
└────────┬────────┘
         │ bash cicd/deploy.sh
         ▼
┌─────────────────┐
│  deploy.sh       │
│  1. git pull     │
│  2. 增量编译      │  ← 只编译变更的模块
│  3. 重启后端      │
│  4. 重载 Nginx   │
└─────────────────┘
```

---

## 2. 一次配置，终身自动

### 2.1 服务器安装依赖

```bash
ssh root@你的服务器

# 安装 PM2（守护 webhook 进程）
npm install -g pm2

# 创建日志目录
mkdir -p /root/finding/deploy/cicd/logs
```

### 2.2 配置 Webhook 密钥

```bash
cd /root/finding/deploy/cicd

# 编辑 webhook-server.js，修改 SECRET
vim webhook-server.js
# 把 your-webhook-secret-change-me 改成一个随机字符串
# 比如: SECRET = 'f1nd1ng-C1CD-s3cr3t-2026';
```

也记下来，待会配 Gitee 要用。

### 2.3 给脚本加执行权限

```bash
chmod +x /root/finding/deploy/cicd/deploy.sh
```

### 2.4 启动 Webhook 服务

```bash
cd /root/finding/deploy/cicd

# 用 PM2 启动
pm2 start ecosystem.config.js

# 设为开机自启
pm2 save
pm2 startup

# 验证
curl http://localhost:9000/health
# 返回 {"status":"ok","uptime":...}
```

### 2.5 防火墙开放端口

```bash
# 让 Gitee 能 POST 到服务器
ufw allow 9000/tcp
```

---

## 3. Gitee 配置

### 3.1 推送代码到 Gitee

```bash
# Windows 本地
cd D:\FullStack\finding

# 添加 Gitee 远程仓库
git remote add gitee https://gitee.com/你的用户名/finding.git

# 推送
git push gitee master
```

### 3.2 添加 Webhook

1. 打开 `https://gitee.com/你的用户名/finding/settings/hooks`
2. 点击 **添加 Webhook**
3. 填写：

| 字段 | 值 |
|------|-----|
| URL | `http://你的服务器IP:9000/webhook` |
| 密码/签名密钥 | `f1nd1ng-C1CD-s3cr3t-2026`（你在 webhook-server.js 里设的那个） |
| 触发事件 | ☑ Push（勾选这个就够了） |

4. 点击 **添加**
5. 点击 **测试** → 看服务器日志 `pm2 logs finding-webhook`

如果测试显示 "签名验证失败" → 检查 SECRET 是否一致
如果返回 200 → 配置成功！

---

## 4. 日常使用

```bash
# Windows 本地写代码 → 提交 → 推送
git add .
git commit -m "feat: 新功能"
git push gitee master

# 之后什么都不用管，服务器自动：
# 1. 拉取最新代码
# 2. 增量构建（只编译改了的部分）
# 3. 重启后端（不影响用户，3 秒完成）
# 4. 重载 Nginx
```

查看部署日志：
```bash
ssh root@服务器
pm2 logs finding-webhook           # 实时日志
tail -f /root/finding/deploy/app.log   # 后端日志
```

---

## 5. 高级：增量编译策略

`deploy.sh` 会根据变更文件决定编译哪些模块：

| 变更文件路径 | 动作 |
|-------------|------|
| `finding-server/**` | 重新编译 jar + 重启后端 |
| `finding-web/**` | 重新构建学生端 + 重载 Nginx |
| `finding-admin/**` | 重新构建管理端 + 重载 Nginx |
| `deploy/**` | 仅 git pull，不触发构建 |

> 只改前端 → 后端不重启，秒级完成；只改后端 → 前端不构建。

---

## 6. 备选方案：不用 Webhook，用 crontab 定时检查

如果因为网络/安全原因 Gitee Webhook 访问不了服务器（比如服务器在家里），可以用定时检查：

```bash
# 每 5 分钟检查一次是否有新代码
crontab -e
# 添加:
*/5 * * * * bash /root/finding/deploy/cicd/deploy.sh >> /root/finding/deploy/cicd/logs/cron.log 2>&1
```

> 缺点：有 5 分钟延迟。适合不要求实时更新的项目。

---

## 7. 常用命令速查

```bash
# Webhook 服务管理
pm2 status                        # 查看状态
pm2 logs finding-webhook          # 实时日志
pm2 restart finding-webhook       # 重启服务
pm2 stop finding-webhook          # 停止服务
pm2 delete finding-webhook        # 卸载

# 手动触发布署
bash /root/finding/deploy/cicd/deploy.sh

# 查看最近部署日志
tail -50 /root/finding/deploy/cicd/logs/out.log
tail -50 /root/finding/deploy/cicd/logs/error.log
```

---

## 8. 故障排查

### Webhook 测试返回 403 Forbidden
→ 密码/签名密钥不一致，Gitee 填的和 webhook-server.js 里的 `SECRET` 必须完全一致

### Webhook 测试返回 200 但没触发部署
→ `pm2 logs finding-webhook` 查看日志，检查 `deploy.sh` 路径是否正确

### git pull 失败
→ 确认服务器上 `/root/finding` 是 git 仓库且 remote 指向 Gitee：
```bash
cd /root/finding && git remote -v
# origin  https://gitee.com/你的用户名/finding.git
```

### 端口 9000 不通
→ 云服务器需要在**安全组**中放行 9000 端口（不只是 ufw）
→ 阿里云/腾讯云：控制台 → 安全组 → 添加入站规则 TCP:9000
