/**
 * Finding CI/CD — Gitee Webhook 接收服务
 *
 * 部署到服务器后运行: node cicd/webhook-server.js
 * Gitee 推送代码时会 POST 到此服务，自动触发部署。
 *
 * 安全: 通过 SECRET 签名验证请求来源。
 * 保活: 使用 PM2 或 systemd 保持进程运行。
 */

const http = require('http');
const crypto = require('crypto');
const { exec } = require('child_process');

// ==================== 配置 ====================
const PORT = 9000;                          // Webhook 监听端口
const SECRET = 'your-webhook-secret-change-me';  // 与 Gitee 配置一致
const DEPLOY_SCRIPT = '/root/finding/deploy/cicd/deploy.sh';
// ============================================

function verifySignature(payload, signature) {
    const expected = crypto.createHmac('sha256', SECRET).update(payload).digest('hex');
    return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(signature));
}

function log(msg) {
    console.log(`[${new Date().toISOString()}] ${msg}`);
}

function runDeploy(branch) {
    log(`🚀 开始部署 (branch: ${branch})`);
    exec(`bash ${DEPLOY_SCRIPT} ${branch}`, { timeout: 600000 }, (err, stdout, stderr) => {
        if (err) {
            log(`❌ 部署失败: ${err.message}`);
            if (stderr) log(`stderr: ${stderr}`);
            return;
        }
        log(stdout.replace(/\n/g, ' | '));
        log('✅ 部署完成');
    });
}

const server = http.createServer((req, res) => {
    // 健康检查
    if (req.method === 'GET' && req.url === '/health') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: 'ok', uptime: process.uptime() }));
        return;
    }

    // 只处理 Gitee Webhook POST
    if (req.method !== 'POST' || req.url !== '/webhook') {
        res.writeHead(404);
        res.end('Not Found');
        return;
    }

    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
        // 验证签名
        const signature = req.headers['x-gitee-token'] || '';
        if (SECRET !== 'your-webhook-secret-change-me' && !verifySignature(body, signature)) {
            log('⚠️  签名验证失败，忽略请求');
            res.writeHead(403);
            res.end('Forbidden');
            return;
        }

        try {
            const data = JSON.parse(body);
            const branch = (data.ref || '').replace('refs/heads/', '') || 'master';
            const pusher = data.pusher?.name || 'unknown';
            const commits = data.commits?.length || 0;

            log(`📨 收到推送: ${pusher} → ${branch} (${commits} commits)`);

            // 异步部署，立即响应 Gitee
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ status: 'deploying', branch }));

            runDeploy(branch);
        } catch (e) {
            log(`❌ 解析请求失败: ${e.message}`);
            res.writeHead(400);
            res.end('Bad Request');
        }
    });
});

server.listen(PORT, () => {
    log(`🔌 Webhook 服务已启动 :${PORT}`);
    log(`   健康检查: http://localhost:${PORT}/health`);
    log(`   Webhook:  http://localhost:${PORT}/webhook`);
});
