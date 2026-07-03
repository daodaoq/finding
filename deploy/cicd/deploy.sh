#!/bin/bash
# ============================================================
# Finding CI/CD — 服务器端自动部署脚本
# 由 webhook 监听器或 crontab 触发
# 用法: ./cicd/deploy.sh [branch:master]
# ============================================================
set -e

BRANCH="${1:-master}"
PROJECT_DIR="/root/finding"
DEPLOY_DIR="${PROJECT_DIR}/deploy"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "[$(date '+%H:%M:%S')] $1"; }

log "${GREEN}=== Finding CI/CD 自动部署开始 (分支: ${BRANCH}) ===${NC}"

# 1. 拉取最新代码
cd "$PROJECT_DIR"
log "拉取 Gitee 最新代码..."
git fetch origin "$BRANCH"
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse "origin/$BRANCH")

if [ "$LOCAL" = "$REMOTE" ]; then
    log "${YELLOW}代码无变化，跳过部署${NC}"
    exit 0
fi

log "检测到新提交: ${REMOTE:0:8}"
git pull origin "$BRANCH"

# 2. 加载环境变量
if [ -f "$DEPLOY_DIR/.env" ]; then
    set -a; source "$DEPLOY_DIR/.env"; set +a
fi

# 3. 判断哪些模块有变更
CHANGED_FILES=$(git diff --name-only "$LOCAL" "$REMOTE" 2>/dev/null || echo "")

NEED_BACKEND=false
NEED_WEB=false
NEED_ADMIN=false

if echo "$CHANGED_FILES" | grep -q "^finding-server/"; then
    NEED_BACKEND=true
fi
if echo "$CHANGED_FILES" | grep -q "^finding-web/"; then
    NEED_WEB=true
fi
if echo "$CHANGED_FILES" | grep -q "^finding-admin/"; then
    NEED_ADMIN=true
fi

# 首次运行全量构建
if [ -z "$CHANGED_FILES" ]; then
    NEED_BACKEND=true
    NEED_WEB=true
    NEED_ADMIN=true
fi

# 4. 增量构建
if $NEED_WEB; then
    log "🔨 构建学生端..."
    cd "$PROJECT_DIR/finding-web"
    npm ci --silent 2>&1 | tail -1
    npm run build 2>&1 | tail -3
    log "${GREEN}学生端构建完成${NC}"
fi

if $NEED_ADMIN; then
    log "🔨 构建管理端..."
    cd "$PROJECT_DIR/finding-admin"
    npm ci --silent 2>&1 | tail -1
    npx vite build --base=/admin/ 2>&1 | tail -3
    log "${GREEN}管理端构建完成${NC}"
fi

if $NEED_BACKEND; then
    log "🔨 构建后端..."
    cd "$PROJECT_DIR/finding-server"
    mvn clean package -DskipTests -q 2>&1 | tail -3
    log "${GREEN}后端构建完成${NC}"

    # 重启后端
    if [ -f "$DEPLOY_DIR/app.pid" ] && kill -0 $(cat "$DEPLOY_DIR/app.pid") 2>/dev/null; then
        log "重启后端服务..."
        kill $(cat "$DEPLOY_DIR/app.pid")
        sleep 3
    fi

    cd "$DEPLOY_DIR"
    JAR_FILE=$(ls "$PROJECT_DIR/finding-server/target/finding-server-"*.jar 2>/dev/null | head -1)
    nohup java -Xmx256m -jar "$JAR_FILE" \
        --spring.profiles.active=prod \
        --server.port=8080 \
        > "$DEPLOY_DIR/app.log" 2>&1 &
    echo $! > "$DEPLOY_DIR/app.pid"
    log "${GREEN}后端已重启 (PID: $(cat $DEPLOY_DIR/app.pid))${NC}"

    # 等待后端就绪
    for i in $(seq 1 20); do
        if curl -s http://localhost:8080/api/v1 >/dev/null 2>&1; then
            log "${GREEN}后端就绪${NC}"
            break
        fi
        sleep 2
    done
fi

# 5. 重载 Nginx
if $NEED_WEB || $NEED_ADMIN; then
    docker exec finding-nginx nginx -s reload 2>/dev/null || true
    log "${GREEN}Nginx 已重载${NC}"
fi

log "${GREEN}=== 部署完成 ===${NC}"
