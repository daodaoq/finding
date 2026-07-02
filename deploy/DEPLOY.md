# Finding 大学生社交平台 — 生产部署指南

> 从 Windows 开发机到 Linux 云服务器，含 Docker 中间件 + Nginx 前端托管 + 数据迁移

---

## 目录

1. [架构概览](#1-架构概览)
2. [Windows 本机准备](#2-windows-本机准备)
3. [文件上传到服务器](#3-文件上传到服务器)
4. [服务器环境安装](#4-服务器环境安装)
5. [一键部署](#5-一键部署)
6. [手动部署（分步）](#6-手动部署分步)
7. [配置 HTTPS](#7-配置-https可选)
8. [常用运维命令](#8-常用运维命令)
9. [故障排查](#9-故障排查)

---

## 1. 架构概览

```
┌──────────────────────────────────────────────────────────────┐
│                         Nginx :80                            │
│  /            → 学生端 SPA (finding-web/dist)                 │
│  /admin/*     → 管理端 SPA (finding-admin/dist)               │
│  /api/*       → 后端 API (localhost:8080)                     │
│  /ws          → WebSocket (localhost:8080)                    │
│  /uploads/*   → 后端静态资源 (localhost:8080)                  │
└───────────────────┬──────────────────────────────────────────┘
                    │
┌───────────────────▼──────────────────────────────────────────┐
│          Spring Boot :8080 (宿主机 Java 进程)                  │
│          java -jar finding-server-1.0.0.jar                   │
└──┬──────────┬──────────┬──────────┬──────────────────────────┘
   │          │          │          │
┌──▼──┐  ┌───▼───┐  ┌──▼───┐  ┌──▼──────┐
│MySQL│  │ Redis │  │Rabbit│  │  MinIO  │
│3306 │  │ 6379  │  │ 5672 │  │9000/9001│
│Docker│ │Docker │  │Docker│  │ Docker  │
└─────┘  └───────┘  └──────┘  └─────────┘
```

| 组件 | 部署方式 | 端口 | 访问地址 |
|------|---------|------|---------|
| Nginx | Docker 容器 | 80 / 443 | — |
| **学生端** | Nginx 静态托管 | — | `http://IP/` |
| **管理端** | Nginx 静态托管 | — | `http://IP/admin` |
| Spring Boot | 宿主机 Java 进程 | 8080 | `http://IP:8080` |
| MySQL 8.0 | Docker 容器 | 3306 | (仅本地) |
| Redis 7 | Docker 容器 | 6379 | (仅本地) |
| RabbitMQ 3.12 | Docker 容器 | 5672 / 15672 | `http://IP:15672` |
| MinIO | Docker 容器 | 9000 / 9001 | `http://IP:9001` |

### 双前端说明

| 前端 | 框架 | 构建命令 | 部署路径 |
|------|------|---------|---------|
| finding-web (学生端) | React + Zustand + Vite | `npm run build` | `/usr/share/nginx/html/` |
| finding-admin (管理端) | React + Ant Design + Vite | `vite build --base=/admin/` | `/usr/share/nginx/admin/` |

> **关键**: 管理端构建时必须传 `--base=/admin/`，确保 JS/CSS 资源路径和 React Router 都使用 `/admin/` 前缀。

---

## 2. Windows 本机准备

### 2.1 导出数据库

双击运行 `deploy\export-db.bat`，或手动执行：

```cmd
cd D:\FullStack\finding\deploy

mysqldump -uroot -p123456 -hlocalhost -P3306 ^
  --complete-insert --routines --triggers finding > init-data-full.sql
```

会生成两个文件：
- `init-structure.sql` — 仅表结构
- `init-data-full.sql` — 完整数据（**用这个**）

### 2.2 确认文件清单

部署需要上传到服务器的文件：

```
finding/                            # 整个项目目录（或至少以下部分）
├── deploy/                         # ★ 部署配置（上传到服务器）
│   ├── .env.example                #   环境变量模板
│   ├── docker-compose.yml          #   Docker 编排
│   ├── init.sql                    #   数据库初始化 SQL
│   ├── init-data-full.sql          #   完整数据导出（覆盖 init.sql）
│   ├── deploy.sh                   #   一键部署脚本
│   └── nginx/
│       ├── nginx.conf              #   Nginx 配置
│       └── ssl/                    #   SSL 证书目录（后面放证书）
├── finding-server/                 # Spring Boot 后端（需编译）
├── finding-web/                    # React 学生端（需编译）
└── finding-admin/                  # React 管理端（需编译）
```

> **最小上传方案**：`deploy/` + `finding-server/` + `finding-web/` + `finding-admin/` 四个目录。

### 2.3 将数据文件替换为完整版

```cmd
cd D:\FullStack\finding\deploy
copy /Y init-data-full.sql init.sql
```

这样 Docker 首次启动 MySQL 时会自动导入结构和数据。

### 2.4 验证管理端构建（可选）

在本机测试管理端能否正常构建：

```cmd
cd D:\FullStack\finding\finding-admin
npm ci
npx vite build --base=/admin/
# 检查 dist/ 目录是否生成
```

---

## 3. 文件上传到服务器

### 方式 A：scp 上传（推荐）

在 Windows 终端（PowerShell / CMD / Git Bash）执行：

```bash
# 设置你的服务器 IP
SET SERVER_IP=你的服务器IP

# 先打包（排除不需要的目录）
cd D:\FullStack\finding
tar -czf finding-deploy.tar.gz ^
    --exclude=node_modules ^
    --exclude=target ^
    --exclude=.git ^
    --exclude=*.log ^
    deploy/ finding-server/ finding-web/ finding-admin/

# 上传
scp finding-deploy.tar.gz root@%SERVER_IP%:/root/

# SSH 登录服务器解压
ssh root@%SERVER_IP%
cd /root
tar -xzf finding-deploy.tar.gz
```

### 方式 B：Git 拉取

如果代码已经推送到 GitHub：

```bash
ssh root@你的服务器IP
cd /root
git clone https://github.com/daodaoq/finding.git
cd finding
# 把 deploy/init-data-full.sql 额外上传（Git 不追踪数据库数据）
```

### 方式 C：使用 FTP 工具

用 FileZilla / WinSCP 等工具直接拖拽 `finding/` 整个目录到服务器 `/root/finding/`。

---

## 4. 服务器环境安装

在服务器上以 `root` 执行以下命令：

### 4.1 安装 Docker

```bash
# 官方一键安装
curl -fsSL https://get.docker.com | sh

# 启动 Docker
systemctl enable docker
systemctl start docker

# 验证
docker --version
docker compose version
```

### 4.2 安装 Java 17

```bash
# Ubuntu / Debian
apt update && apt install -y openjdk-17-jdk

# CentOS / Rocky Linux
dnf install -y java-17-openjdk

# 验证
java -version   # 应显示 17.x
```

### 4.3 安装 Node.js 18+（构建前端用）

```bash
# 使用 NodeSource
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs

# 验证
node --version   # ≥ 18
npm --version
```

### 4.4 安装 Maven（或使用项目自带的 mvnw）

```bash
apt install -y maven
mvn --version
```

### 4.5 配置防火墙

```bash
# 开放端口
ufw allow 80/tcp       # Nginx HTTP（学生端+管理端）
ufw allow 443/tcp      # Nginx HTTPS（配置 SSL 后）
ufw allow 8080/tcp     # 后端 API（可选，仅调试用）
# 以下端口仅本地访问，不需要开放：
# 3306 (MySQL), 6379 (Redis), 5672 (RabbitMQ), 9000-9001 (MinIO)

ufw enable
```

---

## 5. 一键部署

```bash
cd /root/finding/deploy

# 1. 配置环境变量
cp .env.example .env
vim .env    # ★ 务必修改所有密码和 JWT 密钥！

# 2. 如果之前导出了完整数据，替换 init.sql
cp init-data-full.sql init.sql

# 3. 给脚本执行权限
chmod +x deploy.sh

# 4. 一键部署！
./deploy.sh
```

脚本自动完成：
1. ✅ 检查 Java / Node / Docker / Maven
2. ✅ 加载 `.env` 配置
3. ✅ 构建学生端 `npm run build` → `finding-web/dist/`
4. ✅ 构建管理端 `vite build --base=/admin/` → `finding-admin/dist/`
5. ✅ 构建后端 `mvn package` → `finding-server-*.jar`
6. ✅ 拉取并启动 Docker 中间件
7. ✅ 等待 MySQL / Redis / RabbitMQ 健康检查通过
8. ✅ 创建 MinIO Bucket
9. ✅ 启动 Spring Boot 后端
10. ✅ 重载 Nginx

---

## 6. 手动部署（分步）

如果一键脚本出现问题，分步执行：

### 6.1 配置环境变量

```bash
cd /root/finding/deploy
cp .env.example .env
vim .env    # 改密码
```

### 6.2 启动 Docker 中间件

```bash
cd /root/finding/deploy
docker compose up -d

# 查看状态
docker compose ps

# 等待 MySQL 就绪
docker exec finding-mysql mysqladmin ping -h localhost -u root -p
```

### 6.3 构建学生端

```bash
cd /root/finding/finding-web
npm ci
npm run build
# 产物在 dist/ 目录
```

### 6.4 构建管理端

```bash
cd /root/finding/finding-admin
npm ci

# ★ 必须传 --base=/admin/ 以匹配 Nginx 路径
npx vite build --base=/admin/

# 产物在 dist/ 目录
```

### 6.5 构建后端

```bash
cd /root/finding/finding-server
mvn clean package -DskipTests
# 产物在 target/finding-server-1.0.0-SNAPSHOT.jar
```

### 6.6 启动后端

```bash
# 加载环境变量
cd /root/finding/deploy
set -a; source .env; set +a

# 启动
cd /root/finding/finding-server
nohup java -jar target/finding-server-*.jar \
    --spring.profiles.active=prod \
    --server.port=8080 \
    > /root/finding/deploy/app.log 2>&1 &

echo $! > /root/finding/deploy/app.pid

# 验证
curl http://localhost:8080/api/v1
```

### 6.7 启动/重载 Nginx

```bash
docker compose -f /root/finding/deploy/docker-compose.yml up -d nginx

# 或重载配置
docker exec finding-nginx nginx -s reload
```

### 6.8 访问验证

| 页面 | 地址 |
|------|------|
| 学生端 | `http://你的服务器IP/` |
| 管理端 | `http://你的服务器IP/admin` |
| API | `http://你的服务器IP/api/v1` |

```bash
# 本地测试
curl http://localhost/              # 学生端 → 返回 HTML
curl http://localhost/admin/        # 管理端 → 返回 HTML
curl http://localhost/api/v1        # API → 返回 JSON
```

---

## 7. 配置 HTTPS（可选）

### 7.1 使用 Let's Encrypt 免费证书

```bash
# 安装 certbot
apt install -y certbot

# 先确保域名 DNS 已指向服务器 IP
# 暂停 Nginx
docker stop finding-nginx

# 申请证书
certbot certonly --standalone -d your-domain.com

# 证书路径
# /etc/letsencrypt/live/your-domain.com/fullchain.pem
# /etc/letsencrypt/live/your-domain.com/privkey.pem
```

### 7.2 复制证书到 deploy 目录

```bash
cp /etc/letsencrypt/live/your-domain.com/fullchain.pem /root/finding/deploy/nginx/ssl/
cp /etc/letsencrypt/live/your-domain.com/privkey.pem /root/finding/deploy/nginx/ssl/
```

### 7.3 启用 HTTPS 配置

编辑 `deploy/nginx/nginx.conf`：

1. 取消 HTTP→HTTPS 重定向的注释（第一个 server 块）
2. 取消 HTTPS server 块的注释
3. **注意**: HTTPS server 块也需包含 `/admin` location 配置
4. 将 `your-domain.com` 替换为实际域名

然后重载 Nginx：

```bash
docker exec finding-nginx nginx -s reload
```

### 7.4 自动续期

```bash
# 添加 crontab 定时任务
crontab -e

# 每月 1 号凌晨 3 点续期
0 3 1 * * certbot renew --quiet --pre-hook "docker stop finding-nginx" --post-hook "docker start finding-nginx"
```

---

## 8. 常用运维命令

### Docker 中间件管理

```bash
cd /root/finding/deploy

# 查看所有容器状态
docker compose ps

# 查看某个服务日志
docker logs finding-mysql    --tail 50
docker logs finding-redis    --tail 50
docker logs finding-rabbitmq --tail 50
docker logs finding-minio    --tail 50
docker logs finding-nginx    --tail 50

# 重启某个服务
docker compose restart mysql
docker compose restart redis

# 停止所有中间件
docker compose down

# 启动所有中间件
docker compose up -d

# 备份数据库
docker exec finding-mysql mysqldump -uroot -p finding > backup_$(date +%Y%m%d).sql
```

### 后端管理

```bash
# 查看后端日志
tail -f /root/finding/deploy/app.log

# 查看最近 100 行
tail -100 /root/finding/deploy/app.log

# 停止后端
kill $(cat /root/finding/deploy/app.pid)

# 重启后端
cd /root/finding/deploy
set -a; source .env; set +a
nohup java -jar /root/finding/finding-server/target/finding-server-*.jar \
    --spring.profiles.active=prod --server.port=8080 \
    > /root/finding/deploy/app.log 2>&1 &
echo $! > /root/finding/deploy/app.pid
```

### Nginx 管理

```bash
# 测试配置
docker exec finding-nginx nginx -t

# 重载配置（不中断服务）
docker exec finding-nginx nginx -s reload

# 查看访问日志
docker exec finding-nginx tail -100 /var/log/nginx/access.log

# 查看错误日志
docker exec finding-nginx tail -50 /var/log/nginx/error.log
```

### 更新部署

```bash
cd /root/finding

# 拉取最新代码
git pull

# 重新构建学生端
cd finding-web && npm ci && npm run build

# 重新构建管理端
cd ../finding-admin && npm ci && npx vite build --base=/admin/

# 重新构建后端
cd ../finding-server && mvn clean package -DskipTests

# 重启后端
kill $(cat ../deploy/app.pid)
cd ../deploy
set -a; source .env; set +a
nohup java -jar ../finding-server/target/finding-server-*.jar \
    --spring.profiles.active=prod --server.port=8080 \
    > app.log 2>&1 &
echo $! > app.pid

# 重载 Nginx
docker exec finding-nginx nginx -s reload
```

---

## 9. 故障排查

### 9.1 前端页面 404

**现象**：访问 `http://IP` 显示 Nginx 默认页面

**排查**：
```bash
# 检查学生端 dist 目录是否存在
ls -la /root/finding/finding-web/dist/

# 检查 Nginx 挂载
docker exec finding-nginx ls /usr/share/nginx/html/

# 如果没有 index.html，检查 docker-compose.yml 的 volumes 挂载路径
```

### 9.2 管理端 404 或白屏

**现象**：访问 `http://IP/admin` 返回 404，或页面白屏 JS 报错

**排查**：
```bash
# 检查管理端 dist 目录
ls -la /root/finding/finding-admin/dist/

# 检查 Nginx 挂载
docker exec finding-nginx ls /usr/share/nginx/admin/

# 检查 index.html 中的资源路径是否正确
docker exec finding-nginx cat /usr/share/nginx/admin/index.html | grep -E "src=|href="
# 资源路径应该以 /admin/assets/ 开头

# 如果不是，说明构建时未传 --base=/admin/
```

### 9.3 管理端登录后跳转错误

**现象**：管理端登录后跳转到 `/login` 而不是 `/admin/login`

**排查**：
```bash
# 检查 BrowserRouter basename 是否设置为 /admin
grep -r "basename" /root/finding/finding-admin/src/App.tsx
# 应该有: <BrowserRouter basename="/admin">

# 检查 API 拦截器中的跳转路径
grep "window.location.href" /root/finding/finding-admin/src/api/request.ts
# 应该是 /admin/login 而不是 /login
```

### 9.4 API 502 Bad Gateway

**现象**：前端正常但 API 请求返回 502

**排查**：
```bash
# 检查后端是否在运行
curl http://localhost:8080/api/v1

# 如果没响应，检查日志
tail -50 /root/finding/deploy/app.log

# 常见原因：MySQL/Redis 连不上
docker compose ps    # 确认中间件都在运行
```

### 9.5 WebSocket 连接失败

**现象**：聊天消息不能实时推送

**排查**：
```bash
# 检查 Nginx WebSocket 配置
docker exec finding-nginx cat /etc/nginx/nginx.conf | grep -A 10 "/ws"

# 确保有这三行：
# proxy_http_version 1.1;
# proxy_set_header Upgrade $http_upgrade;
# proxy_set_header Connection "upgrade";
```

### 9.6 MinIO 上传失败

**现象**：头像/图片上传后无法显示

**排查**：
```bash
# MinIO 控制台
http://服务器IP:9001    # 用 .env 中的账号密码登录

# 检查 Bucket 是否存在
# 如果没有 finding bucket，手动创建并设置为公开读

# 或者用 mc 命令
docker exec finding-minio mc mb local/finding
docker exec finding-minio mc anonymous set download local/finding
```

### 9.7 MySQL 连接拒绝

**现象**：后端日志显示 `Communications link failure`

**排查**：
```bash
# 检查 MySQL 容器状态
docker logs finding-mysql --tail 20

# 进入容器测试
docker exec -it finding-mysql mysql -uroot -p
# 输入密码后执行: SHOW DATABASES; USE finding; SHOW TABLES;
```

### 9.8 端口冲突

**现象**：容器启动失败，提示端口已被占用

**排查**：
```bash
# 查看端口占用
netstat -tlnp | grep -E "80|3306|6379|5672|9000"

# 停止占用进程或修改 docker-compose.yml 端口映射
```

---

## 附录：目录结构总览

```
/root/finding/
├── deploy/
│   ├── .env                    # 生产环境变量（不上传 Git）
│   ├── .env.example            # 环境变量模板
│   ├── docker-compose.yml      # Docker 中间件编排
│   ├── init.sql                # 数据库初始化（含数据）
│   ├── deploy.sh               # 一键部署脚本
│   ├── app.log                 # 后端运行日志
│   ├── app.pid                 # 后端进程 PID
│   └── nginx/
│       ├── nginx.conf          # Nginx 配置
│       └── ssl/                # SSL 证书
│           ├── fullchain.pem
│           └── privkey.pem
├── finding-server/             # Spring Boot 后端
│   ├── src/
│   ├── pom.xml
│   └── target/
│       └── finding-server-1.0.0-SNAPSHOT.jar
├── finding-web/                # React 学生端
│   ├── src/
│   ├── package.json
│   └── dist/                   # 构建产物 → Nginx /usr/share/nginx/html/
└── finding-admin/              # React 管理端
    ├── src/
    ├── package.json
    └── dist/                   # 构建产物 → Nginx /usr/share/nginx/admin/
```

### Nginx 路由规则

| 请求路径 | 处理方式 | 文件来源 |
|---------|---------|---------|
| `/` | SPA → `index.html` | `finding-web/dist/` |
| `/admin` | SPA → `admin/index.html` | `finding-admin/dist/` |
| `/admin/dashboard` | SPA → `admin/index.html` | `finding-admin/dist/` |
| `/admin/users` | SPA → `admin/index.html` | `finding-admin/dist/` |
| `/api/*` | 反代 → `localhost:8080` | Spring Boot |
| `/ws` | WebSocket → `localhost:8080` | Spring Boot |
| `/uploads/*` | 反代 → `localhost:8080` | Spring Boot |
