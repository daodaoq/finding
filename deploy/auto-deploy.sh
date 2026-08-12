#!/bin/bash
# ============================================================
# Finding - 自动部署脚本（由 webhook_server.py 触发）
# ------------------------------------------------------------
# 作用：把 GitHub 最新代码拉下来 → 跑 deploy.sh 完整构建并部署。
# 流程：git pull → deploy.sh（构建前后端 + 起容器 + 重启后端 + 重载 nginx）
#
# 【生产 Agent 维护要点】
#   - 单实例锁：上一次部署没结束时不重复触发（flock）
#   - 拉取用 --ff-only：本地有改动导致无法快进时立即失败，避免覆盖本地修改
#   - 失败即退出（set -e），退出码会被 webhook_server.py 记录到 deploy.log
#
# 手动触发（排查用）：
#   ./auto-deploy.sh              # 默认拉取 master
#   ./auto-deploy.sh dev          # 拉取 dev 分支
# ============================================================
set -e

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$DEPLOY_DIR")"

# 传入的分支名（webhook 透传），默认 master
BRANCH="${1:-master}"
LOCK_FILE="$DEPLOY_DIR/.auto-deploy.lock"

# ---------- 单实例锁：避免并发部署 ----------
# exec 9> 打开一个文件描述符持有锁，直到脚本结束才释放
exec 9>"$LOCK_FILE"
if ! flock -n 9; then
    echo "[auto-deploy] $(date '+%F %T') 已有部署在进行中，本次触发跳过。"
    exit 0
fi

echo ""
echo "============================================================"
echo "  auto-deploy 开始: $(date '+%F %T')  分支=${BRANCH}"
echo "============================================================"

# ---------- 1. 拉取最新代码 ----------
cd "$PROJECT_DIR"
echo "[auto-deploy] 拉取最新代码 (git pull --ff-only origin ${BRANCH})..."
git fetch origin "$BRANCH"
git pull --ff-only origin "$BRANCH" || {
    echo "[auto-deploy] 拉取失败。可能原因：本地有未提交改动、或当前不在 ${BRANCH} 分支。"
    echo "[auto-deploy] 可手动处理：cd $PROJECT_DIR && git status"
    exit 1
}
echo "[auto-deploy] 当前提交: $(git log -1 --oneline)"

# ---------- 2. 完整构建 + 部署 ----------
cd "$DEPLOY_DIR"
echo "[auto-deploy] 开始构建部署（deploy.sh）..."
./deploy.sh

echo ""
echo "============================================================"
echo "  auto-deploy 完成: $(date '+%F %T')  ✅"
echo "============================================================"
