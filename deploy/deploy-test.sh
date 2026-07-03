#!/bin/bash
# ============================================================
# Finding 测试环境部署脚本 (2C2GB 低配服务器)
# 用法: chmod +x deploy-test.sh && ./deploy-test.sh
# ============================================================
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()  { echo -e "${CYAN}[INFO]${NC}  $1"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
err()   { echo -e "${RED}[ERR]${NC}   $1"; }

# ==================== 切换到脚本目录 ====================
cd "$(dirname "$0")"
DEPLOY_DIR=$(pwd)
PROJECT_DIR=$(dirname "$DEPLOY_DIR")

info "测试环境部署 (docker-compose.test.yml)"
info "部署目录: ${DEPLOY_DIR}"

# ==================== 1. 检查必需工具 ====================
echo ""
info "=== 第 1 步：检查运行环境 ==="

command -v java  >/dev/null 2>&1 || { err "未安装 Java 17+，请先安装: apt install openjdk-17-jdk"; exit 1; }
command -v node  >/dev/null 2>&1 || { err "未安装 Node.js 18+，请先安装"; exit 1; }
command -v npm   >/dev/null 2>&1 || { err "未安装 npm，请先安装 Node.js"; exit 1; }
command -v docker >/dev/null 2>&1 || { err "未安装 Docker，请先安装: curl -fsSL https://get.docker.com | sh"; exit 1; }

if docker compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE="docker-compose"
else
    err "未找到 docker compose，请安装 Docker Compose"
    exit 1
fi

if command -v mvn >/dev/null 2>&1; then
    MVN="mvn"
elif [ -f "$PROJECT_DIR/finding-server/mvnw" ]; then
    MVN="$PROJECT_DIR/finding-server/mvnw"
    chmod +x "$MVN"
else
    err "未找到 Maven，请安装: apt install maven"
    exit 1
fi

ok "Java:   $(java -version 2>&1 | head -1)"
ok "Node:   $(node --version)"
ok "npm:    $(npm --version)"
ok "Docker: $(docker --version)"
ok "Maven:  $MVN"

# ==================== 2. 加载环境变量 ====================
echo ""
info "=== 第 2 步：加载配置 ==="

if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        warn ".env 不存在，从 .env.example 复制默认配置"
        cp .env.example .env
        warn ">>> 请编辑 deploy/.env 中的密码和密钥后重新运行！ <<<"
        exit 1
    else
        err ".env.example 也不存在，请检查部署包完整性"
        exit 1
    fi
fi
set -a; source .env; set +a
ok "环境变量已加载"

# ==================== 3. 构建前端 ====================
echo ""
info "=== 第 3 步：构建前端 ==="

cd "$PROJECT_DIR/finding-web"
info "安装前端依赖..."
npm ci --silent 2>&1 | tail -1
info "构建前端 (Vite)..."
npm run build 2>&1 | tail -5

if [ ! -d dist ]; then
    err "前端构建失败，dist 目录不存在"
    exit 1
fi
ok "前端构建完成 → finding-web/dist/"

# ==================== 3.5 构建管理端 ====================
echo ""
info "=== 第 3.5 步：构建管理端 ==="

cd "$PROJECT_DIR/finding-admin"
info "安装管理端依赖..."
npm ci --silent 2>&1 | tail -1
info "构建管理端 (Vite + Ant Design)..."
npx vite build --base=/admin/ 2>&1 | tail -5

if [ ! -d dist ]; then
    err "管理端构建失败，dist 目录不存在"
    exit 1
fi
ok "管理端构建完成 → finding-admin/dist/"

# ==================== 4. 构建后端 ====================
echo ""
info "=== 第 4 步：构建后端 ==="

cd "$PROJECT_DIR/finding-server"
info "编译 Spring Boot (跳过测试)..."
$MVN clean package -DskipTests -q 2>&1 | tail -3

JAR_FILE=$(ls target/finding-server-*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    err "后端构建失败，未找到 jar 文件"
    exit 1
fi
ok "后端构建完成 → $JAR_FILE"

# ==================== 5. 启动 Docker 中间件 (测试版) ====================
echo ""
info "=== 第 5 步：启动 Docker 中间件（低配测试版） ==="

cd "$DEPLOY_DIR"

info "拉取镜像..."
$DOCKER_COMPOSE -f docker-compose.test.yml pull -q 2>&1 | tail -3

info "启动容器..."
$DOCKER_COMPOSE -f docker-compose.test.yml up -d

ok "容器启动中，等待健康检查..."

# 等待 MySQL 就绪
RETRY=0
MAX_RETRY=30
until docker exec finding-mysql mysqladmin ping -h localhost -u root -p"${MYSQL_ROOT_PASSWORD}" --silent 2>/dev/null; do
    sleep 2
    RETRY=$((RETRY + 1))
    if [ $RETRY -ge $MAX_RETRY ]; then
        err "MySQL 启动超时，请检查: docker logs finding-mysql"
        exit 1
    fi
done
ok "MySQL 已就绪"

# 等待 Redis
until docker exec finding-redis redis-cli -a "${REDIS_PASSWORD}" ping 2>/dev/null | grep -q PONG; do
    sleep 1
done
ok "Redis 已就绪"

# 等待 RabbitMQ
until docker exec finding-rabbitmq rabbitmq-diagnostics check_port_connectivity --silent 2>/dev/null; do
    sleep 1
done
ok "RabbitMQ 已就绪"

ok "所有中间件已启动"

# ==================== 6. 初始化 MinIO Bucket ====================
echo ""
info "=== 第 6 步：初始化 MinIO ==="

sleep 3
if command -v mc >/dev/null 2>&1; then
    mc alias set finding-minio http://localhost:9000 "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" 2>/dev/null || true
    mc mb finding-minio/${MINIO_BUCKET:-finding} --ignore-existing 2>/dev/null || true
    mc anonymous set download finding-minio/${MINIO_BUCKET:-finding} 2>/dev/null || true
    ok "MinIO Bucket '${MINIO_BUCKET:-finding}' 已就绪（公开读）"
else
    warn "未安装 MinIO Client (mc)，请手动创建 Bucket: ${MINIO_BUCKET:-finding}"
    warn "MinIO Console: http://localhost:9001 (${MINIO_ACCESS_KEY}/${MINIO_SECRET_KEY})"
fi

# ==================== 7. 启动后端 ====================
echo ""
info "=== 第 7 步：启动后端服务（低配 JVM） ==="

if [ -f "$DEPLOY_DIR/app.pid" ]; then
    OLD_PID=$(cat "$DEPLOY_DIR/app.pid")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        info "停止旧进程 (PID: $OLD_PID)..."
        kill "$OLD_PID" 2>/dev/null || true
        sleep 2
    fi
fi

# 测试环境用 256MB 堆
nohup java -Xmx256m -jar "$JAR_FILE" \
    --spring.profiles.active=prod \
    --server.port=8080 \
    > "$DEPLOY_DIR/app.log" 2>&1 &

APP_PID=$!
echo $APP_PID > "$DEPLOY_DIR/app.pid"
ok "后端已启动 (PID: $APP_PID, -Xmx256m)"
info "日志文件: $DEPLOY_DIR/app.log"

# 等待后端启动
info "等待后端启动..."
RETRY=0
until curl -s http://localhost:8080/api/v1 >/dev/null 2>&1; do
    sleep 3
    RETRY=$((RETRY + 1))
    if [ $RETRY -ge 20 ]; then
        warn "后端启动超时，请检查日志: tail -f $DEPLOY_DIR/app.log"
        break
    fi
done

if [ $RETRY -lt 20 ]; then
    ok "后端服务已就绪 → http://localhost:8080"
fi

# ==================== 8. Nginx 重载 ====================
echo ""
info "=== 第 8 步：重载 Nginx ==="

docker exec finding-nginx nginx -s reload 2>/dev/null || true
ok "Nginx 已重载"

# ==================== 9. 完成 ====================
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║    🧪 Finding 测试环境部署成功！          ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║${NC}  学生端:     http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'localhost')       ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  管理端:     http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'localhost')/admin ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  API 地址:  http://localhost:8080/api/v1 ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  MinIO:      http://localhost:9001       ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  RabbitMQ:   http://localhost:15672      ${GREEN}║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║${NC}  后端日志:   tail -f $DEPLOY_DIR/app.log ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  容器状态:   $DOCKER_COMPOSE -f docker-compose.test.yml ps ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  停止服务:   $DOCKER_COMPOSE -f docker-compose.test.yml down ${GREEN}║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════╝${NC}"
