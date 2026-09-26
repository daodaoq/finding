#!/bin/bash
# ============================================================
# Finding - 本地增量构建部署脚本（不走 git）
# ------------------------------------------------------------
# 适用场景：直接在服务器上改源码，构建完立即生效。
# 相当于 deploy.sh 的"只做改动那部分"版本：省掉 git 拉取，
# 也省掉无关模块的重复编译。
#
# 用法:
#   ./local-build.sh                  # 等价于 all
#   ./local-build.sh web              # 只构建学生端前端
#   ./local-build.sh admin            # 只构建管理端前端
#   ./local-build.sh server           # 只编译后端并重启服务
#   ./local-build.sh web server       # 可组合多个模块
#   ./local-build.sh all              # 三个都构建
#
# 选项:
#   --install        前端强制重装依赖（npm ci）；默认仅在 node_modules 缺失时安装
#   --clean          后端强制 mvn clean（默认增量编译，快很多）
#   --no-typecheck   前端跳过 tsc 类型检查，只跑 vite build（最快，但会漏掉类型错误）
#   --no-deploy      只构建、不生效（不重载 nginx、不重启后端），用于先验证能否编译通过
#
# 构建日志: deploy/local-build.log（只在失败时打印尾部）
#
# 【两个必须遵守的实现约束】
# 1) 不要写成 `cmd | tail -3` 然后指望 set -e：
#    管道只看最后一个命令的退出码，构建失败会被吞掉，
#    脚本会拿着上一次的旧产物报"成功"。这里一律用 if 包住真实命令。
# 2) 前端不要 `rm -rf dist` 再重建：dist/ 是 nginx 容器的目录 bind mount，
#    绑定的是 inode，删除重建后容器仍指向已删除的旧目录 → 前端立即 404，
#    必须重启容器才能恢复。这里构建到 dist.build/ 再用 rsync 灌进 dist/。
#    顺带也避免了 vite 先清空 dist 再失败导致的线上白屏。
# ============================================================
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info() { echo -e "${CYAN}[INFO]${NC}  $1"; }
ok()   { echo -e "${GREEN}[OK]${NC}    $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $1"; }
err()  { echo -e "${RED}[ERR]${NC}   $1"; }

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$DEPLOY_DIR")"
LOG_FILE="$DEPLOY_DIR/local-build.log"
NGINX_CONTAINER="finding-nginx"
BACKEND_UNIT="finding-backend"

FORCE_INSTALL=0
FORCE_CLEAN=0
TYPECHECK=1
NO_DEPLOY=0
MODULES=()

for arg in "$@"; do
    case "$arg" in
        web|admin|server|all) MODULES+=("$arg") ;;
        --install)      FORCE_INSTALL=1 ;;
        --clean)        FORCE_CLEAN=1 ;;
        --no-typecheck) TYPECHECK=0 ;;
        --no-deploy)    NO_DEPLOY=1 ;;
        -h|--help)
            sed -n '3,23p' "$0" | sed 's/^# \{0,1\}//'
            exit 0 ;;
        *)
            err "未知参数: $arg（可用: web admin server all / --install --clean --no-typecheck --no-deploy）"
            exit 1 ;;
    esac
done

if [ ${#MODULES[@]} -eq 0 ]; then
    MODULES=(all)
fi

DO_WEB=0; DO_ADMIN=0; DO_SERVER=0
for m in "${MODULES[@]}"; do
    case "$m" in
        all)    DO_WEB=1; DO_ADMIN=1; DO_SERVER=1; MODULES=(web admin server) ;;
        web)    DO_WEB=1 ;;
        admin)  DO_ADMIN=1 ;;
        server) DO_SERVER=1 ;;
    esac
done

START_TS=$(date +%s)
echo ""
info "构建模块: ${MODULES[*]}    项目根目录: ${PROJECT_DIR}"

# ---------- Maven ----------
if command -v mvn >/dev/null 2>&1; then
    MVN="mvn"
elif [ -f "$PROJECT_DIR/finding-server/mvnw" ]; then
    MVN="$PROJECT_DIR/finding-server/mvnw"
else
    err "未找到 Maven（mvn 与 finding-server/mvnw 都没有）"
    exit 1
fi

# ---------- 前端依赖 ----------
# 默认只在 node_modules 缺失时安装：npm ci 会删掉整个目录重装，几十秒起步，
# 每次构建都跑纯属浪费。依赖真变了再加 --install。
ensure_deps() {
    local dir="$1" name="$2"
    if [ -d "$dir/node_modules" ] && [ "$FORCE_INSTALL" -eq 0 ]; then
        info "[$name] 依赖已存在，跳过 npm ci（需要重装请加 --install）"
        return 0
    fi
    info "[$name] 安装依赖 (npm ci)..."
    cd "$dir"
    if npm ci > "$LOG_FILE" 2>&1; then
        ok "[$name] 依赖安装完成"
    else
        err "[$name] 依赖安装失败，日志尾部:"
        tail -25 "$LOG_FILE"
        exit 1
    fi
}

# ---------- 前端产物发布 ----------
# dist.build/ 由 vite 生成；rsync 灌进 dist/ 而不是替换 dist/ 本身，
# 因为 dist/ 是 nginx 容器的 bind mount 挂载点（见文件头约束 2）。
publish_dist() {
    local dir="$1" name="$2"
    if [ ! -f "$dir/dist.build/index.html" ]; then
        err "[$name] 构建产物缺失: $dir/dist.build/index.html"
        exit 1
    fi
    mkdir -p "$dir/dist"
    rsync -a --delete "$dir/dist.build/" "$dir/dist/"
    rm -rf "$dir/dist.build"
    ok "[$name] 已发布 → ${dir#"$PROJECT_DIR"/}/dist/（挂载点 inode 未变）"
}

# ---------- 学生端 ----------
build_web() {
    local dir="$PROJECT_DIR/finding-web"
    ensure_deps "$dir" "学生端"
    cd "$dir"
    if [ "$TYPECHECK" -eq 1 ]; then
        info "[学生端] tsc 类型检查 + vite 构建..."
        if { npx tsc -b && npx vite build --outDir dist.build --emptyOutDir; } > "$LOG_FILE" 2>&1; then
            publish_dist "$dir" "学生端"
        else
            err "[学生端] 构建失败，日志尾部:"
            tail -25 "$LOG_FILE"
            exit 1
        fi
    else
        info "[学生端] vite 构建（已跳过类型检查）..."
        if npx vite build --outDir dist.build --emptyOutDir > "$LOG_FILE" 2>&1; then
            publish_dist "$dir" "学生端"
        else
            err "[学生端] 构建失败，日志尾部:"
            tail -25 "$LOG_FILE"
            exit 1
        fi
    fi
}

# ---------- 管理端 ----------
# 与 deploy.sh 一致：管理端走 --base=/admin/，且不做 tsc（沿用线上已验证的方式）
build_admin() {
    local dir="$PROJECT_DIR/finding-admin"
    ensure_deps "$dir" "管理端"
    cd "$dir"
    info "[管理端] vite 构建 (base=/admin/)..."
    if npx vite build --base=/admin/ --outDir dist.build --emptyOutDir > "$LOG_FILE" 2>&1; then
        publish_dist "$dir" "管理端"
    else
        err "[管理端] 构建失败，日志尾部:"
        tail -25 "$LOG_FILE"
        exit 1
    fi
}

# ---------- 后端 ----------
build_server() {
    local dir="$PROJECT_DIR/finding-server"
    local goal="package"
    if [ "$FORCE_CLEAN" -eq 1 ]; then
        goal="clean package"
    fi
    cd "$dir"
    info "[后端] $MVN $goal -DskipTests ..."
    if $MVN $goal -DskipTests -q > "$LOG_FILE" 2>&1; then
        ok "[后端] 编译完成"
    else
        err "[后端] 编译失败，线上仍运行旧 jar。日志尾部:"
        tail -25 "$LOG_FILE"
        exit 1
    fi

    local JAR_FILE
    JAR_FILE=$(ls "$dir"/finding-app/target/finding-app-*.jar 2>/dev/null | head -1)
    if [ -z "$JAR_FILE" ]; then
        err "[后端] 未找到 jar 产物: finding-app/target/finding-app-*.jar"
        exit 1
    fi

    # 编译退出码为 0 但 jar 没更新 = 构建实际没生效，此时重启会继续跑旧代码
    if [ "$(stat -c %Y "$JAR_FILE")" -lt "$START_TS" ]; then
        local newer_src
        newer_src=$(find "$dir" -path '*/src/*' -type f -newer "$JAR_FILE" -print -quit)
        if [ -n "$newer_src" ]; then
            err "[后端] 源码比 jar 新，但 jar 未被重新打包，拒绝继续"
            err "       源码: ${newer_src#"$PROJECT_DIR"/}"
            err "       产物: ${JAR_FILE#"$PROJECT_DIR"/}（mtime $(date -d @"$(stat -c %Y "$JAR_FILE")" '+%F %T')）"
            exit 1
        fi
        warn "[后端] jar 未重新打包（源码无变化），将复用现有产物"
    fi
    ok "[后端] 产物 → ${JAR_FILE#"$PROJECT_DIR"/}"
}

# ---------- 生效：nginx ----------
reload_nginx() {
    if ! docker ps --format '{{.Names}}' | grep -qx "$NGINX_CONTAINER"; then
        warn "nginx 容器 ($NGINX_CONTAINER) 未运行，跳过重载"
        return 0
    fi
    if ! docker exec "$NGINX_CONTAINER" nginx -t >/dev/null 2>&1; then
        err "nginx 配置校验失败，已跳过重载（线上仍用旧配置）:"
        docker exec "$NGINX_CONTAINER" nginx -t 2>&1 | tail -5
        exit 1
    fi
    docker exec "$NGINX_CONTAINER" nginx -s reload >/dev/null 2>&1
    ok "[生效] nginx 已重载"
}

# ---------- 生效：后端 ----------
restart_backend() {
    if ! systemctl cat "$BACKEND_UNIT.service" >/dev/null 2>&1; then
        err "未找到 systemd 服务 $BACKEND_UNIT，请先安装:"
        err "  sudo cp $DEPLOY_DIR/finding-backend.service /etc/systemd/system/ && sudo systemctl daemon-reload"
        exit 1
    fi
    info "[生效] 重启后端 (sudo systemctl restart $BACKEND_UNIT)..."
    sudo systemctl restart "$BACKEND_UNIT"

    # http_code 为 000 表示端口还没监听；任何 HTTP 响应码都说明应用已起来
    local RETRY=0
    while [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/v1)" = "000" ]; do
        sleep 2
        RETRY=$((RETRY + 1))
        if [ "$RETRY" -ge 30 ]; then
            err "后端启动超时，最近日志:"
            journalctl -u "$BACKEND_UNIT" -n 30 --no-pager 2>&1 | tail -30
            exit 1
        fi
    done
    ok "[生效] 后端就绪 (PID $(systemctl show -p MainPID --value "$BACKEND_UNIT")) → http://localhost:8080"
}

# ---------- 执行 ----------
if [ "$DO_WEB" -eq 1 ]; then
    build_web
fi
if [ "$DO_ADMIN" -eq 1 ]; then
    build_admin
fi
if [ "$DO_SERVER" -eq 1 ]; then
    build_server
fi

if [ "$NO_DEPLOY" -eq 1 ]; then
    echo ""
    warn "已指定 --no-deploy：构建产物未生效，线上仍运行旧版本"
    warn "  前端: 稍后执行 nginx 重载即可（或直接重跑本脚本不加 --no-deploy）"
    warn "  后端: 稍后执行 sudo systemctl restart $BACKEND_UNIT"
    echo ""
    ok "构建完成（未部署），耗时 $(( $(date +%s) - START_TS ))s    模块: ${MODULES[*]}"
    exit 0
fi

if [ "$DO_WEB" -eq 1 ] || [ "$DO_ADMIN" -eq 1 ]; then
    reload_nginx
fi
if [ "$DO_SERVER" -eq 1 ]; then
    restart_backend
fi

echo ""
ok "全部完成，耗时 $(( $(date +%s) - START_TS ))s    模块: ${MODULES[*]}"
