# ============================================================
# Finding CI/CD — PM2 配置
# 服务器上执行: pm2 start cicd/ecosystem.config.js
# ============================================================

module.exports = {
    apps: [
        {
            name: 'finding-webhook',
            script: './webhook-server.js',
            cwd: '/root/finding/deploy/cicd',
            // 开机自启
            autorestart: true,
            max_restarts: 10,
            // 日志
            error_file: '/root/finding/deploy/cicd/logs/error.log',
            out_file: '/root/finding/deploy/cicd/logs/out.log',
            log_date_format: 'YYYY-MM-DD HH:mm:ss',
        },
    ],
};
