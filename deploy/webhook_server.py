#!/usr/bin/env python3
# ============================================================
# Finding - Gitee WebHook 接收端（CI/CD 触发入口）
# ------------------------------------------------------------
# 作用：监听 Gitee 仓库的 push 通知，校验签名后触发自动部署。
# 流程：git push → Gitee POST 到本服务 → 校验 token → 触发 auto-deploy.sh
#
# 【生产 Agent 维护要点】
#   - 校验逻辑    → _is_valid_request()
#   - 部署触发    → _handle_push()（放后台执行，立即返回 200，不阻塞 Gitee）
#   - 安全依赖    → GITEE_WEBHOOK_SECRET 必须与 Gitee 仓库 WebHook 的"密码"一致
#   - 分支过滤    → 默认只对 master 分支的 push 生效，可用 GITEE_DEPLOY_BRANCH 改
#   - 部署结果    → 追加写入 webhook_server.log（含 auto-deploy 的退出码）
#
# 运行方式（推荐 systemd，见 finding-webhook.service）：
#   python3 webhook_server.py            # 默认监听 0.0.0.0:8090
# 环境变量：
#   GITEE_WEBHOOK_SECRET   部署密码（必填，与 Gitee WebHook 配置一致）
#   GITEE_DEPLOY_BRANCH    触发分支，默认 master
#   WEBHOOK_PORT           监听端口，默认 8090
#   AUTO_DEPLOY_SCRIPT     auto-deploy.sh 路径，默认同目录
# ============================================================

import os
import hmac
import json
import subprocess
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

# ---------- 配置（可通过环境变量覆盖） ----------
WEBHOOK_SECRET = os.environ.get('GITEE_WEBHOOK_SECRET', '')
TARGET_BRANCH = os.environ.get('GITEE_DEPLOY_BRANCH', 'master')
PORT = int(os.environ.get('WEBHOOK_PORT', '8090'))

_DIR = os.path.dirname(os.path.abspath(__file__))
AUTO_DEPLOY_SCRIPT = os.environ.get(
    'AUTO_DEPLOY_SCRIPT', os.path.join(_DIR, 'auto-deploy.sh'))
LOG_FILE = os.path.join(_DIR, 'webhook_server.log')


def log(msg: str):
    """统一写日志（stdout + 日志文件），便于 agent `tail -f` 观察。"""
    line = f"[{datetime.now():%Y-%m-%d %H:%M:%S}] {msg}"
    print(line, flush=True)
    with open(LOG_FILE, 'a', encoding='utf-8') as f:
        f.write(line + '\n')


def _is_valid_request(query_params: dict, headers: dict) -> bool:
    """
    校验 Gitee WebHook 的"密码"。
    Gitee 有两种携带密码的方式（都兼容）：
      1. URL 查询参数 ?token=<密码>
      2. 请求头 X-Gitee-Token / X-Git-OSChina-Token
    用 hmac.compare_digest 做常数时间比较，防时序攻击。
    """
    if not WEBHOOK_SECRET:
        log("[安全警告] 未配置 GITEE_WEBHOOK_SECRET，所有请求都被拒绝。"
            "请在 .env / systemd 环境里设置后重启本服务。")
        return False

    candidates = []
    candidates += query_params.get('token', [])
    candidates += [headers.get('X-Gitee-Token', ''),
                   headers.get('X-Git-OSChina-Token', '')]

    for c in candidates:
        if c and hmac.compare_digest(c, WEBHOOK_SECRET):
            return True
    return False


def _trigger_deploy(branch: str):
    """
    在后台启动 auto-deploy.sh，并把输出重定向到 deploy.log。
    返回 (exit_code, output)：立即返回，不等待部署完成。
    """
    log(f"触发自动部署: branch={branch}, script={AUTO_DEPLOY_SCRIPT}")
    try:
        # 用 subprocess.Popen 放后台执行；输出写入 deploy.log。
        # 显式用 bash 调用，避免依赖 auto-deploy.sh 的可执行位。
        with open(os.path.join(_DIR, 'deploy.log'), 'ab') as f:
            proc = subprocess.Popen(
                ['bash', AUTO_DEPLOY_SCRIPT, branch],
                stdout=f, stderr=subprocess.STDOUT,
            )
        log(f"自动部署已在后台启动 (PID={proc.pid})，日志: deploy.log")
        return True
    except Exception as e:
        log(f"启动自动部署失败: {e}")
        return False


class WebhookHandler(BaseHTTPRequestHandler):
    """HTTP 处理器：只接收 POST 推送，GET 仅用于健康检查。"""

    def _send(self, code: int, body: str = ''):
        self.send_response(code)
        self.send_header('Content-Type', 'text/plain; charset=utf-8')
        self.end_headers()
        if body:
            self.wfile.write(body.encode('utf-8'))

    def do_GET(self):
        # 健康检查 /health 返回 OK，供监控使用
        if urlparse(self.path).path == '/health':
            self._send(200, 'OK')
        else:
            self._send(404, 'not found')

    def do_POST(self):
        # 1. 读取请求体（Gitee 推送的 JSON）
        length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(length) if length else b''
        query = parse_qs(urlparse(self.path).query)
        headers = {k: v for k, v in self.headers.items()}

        # 2. 校验签名，失败直接 401
        if not _is_valid_request(query, headers):
            log(f"[拒绝] 签名校验失败: from={self.client_address}")
            self._send(401, 'invalid token')
            return

        # 3. 解析事件类型（只处理 push）
        event = (headers.get('X-Gitee-Event') or
                 headers.get('X-Git-OSChina-Event') or '')
        log(f"收到 WebHook: event={event}, from={self.client_address}")

        if event not in ('Push Hook', 'push'):
            self._send(200, 'ignored (not a push event)')
            return

        # 4. 判断分支是否匹配
        try:
            data = json.loads(body or '{}')
            ref = data.get('ref', '')
            branch = ref.rsplit('/', 1)[-1] if ref else ''
        except Exception as e:
            log(f"[警告] 推送 JSON 解析失败: {e}")
            self._send(200, 'ignored (bad payload)')
            return

        if branch and branch != TARGET_BRANCH:
            log(f"[忽略] 分支 {branch} 不是目标分支 {TARGET_BRANCH}")
            self._send(200, 'ignored (branch not match)')
            return

        # 5. 后台触发部署，立即返回
        _trigger_deploy(branch)
        self._send(200, 'deploy triggered')

    def log_message(self, fmt, *args):
        # 关闭默认的访问日志（避免刷屏），由上面的 log() 统一管理
        pass


def main():
    if not WEBHOOK_SECRET:
        log("错误：未设置 GITEE_WEBHOOK_SECRET，拒绝启动（安全要求）。")
        raise SystemExit(1)
    server = ThreadingHTTPServer(('0.0.0.0', PORT), WebhookHandler)
    log(f"Gitee WebHook 接收端已启动: http://0.0.0.0:{PORT}  "
        f"(branch={TARGET_BRANCH}, secret={'已配置' if WEBHOOK_SECRET else '未配置'})")
    log("安全提示：请在 Gitee 仓库『管理 → WebHooks』配置推送地址并填同一密码。")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        log("收到 Ctrl+C，退出。")


if __name__ == '__main__':
    main()
