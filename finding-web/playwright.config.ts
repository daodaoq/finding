import { defineConfig, devices } from '@playwright/test';

/**
 * 冒烟测试 —— 覆盖核心用户路径与后台守卫。
 *
 * 前置条件:后端(finding-server, :8080)及其 MySQL/Redis/RabbitMQ 必须在线。
 * 前端 dev server 由 webServer 自动拉起(已存在则复用)。运行:
 *   npm run test:e2e            # 无头执行全部冒烟
 *   npm run test:e2e:headed     # 有头调试
 *   npm run test:e2e:ui         # UI 模式
 *
 * 测试账号:13096120690 / 12345678(见 finding-server/seed-test-data.sql)
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    locale: 'zh-CN',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: [
    {
      // finding-web 用户端, :3000
      command: 'npm run dev',
      url: 'http://localhost:3000',
      reuseExistingServer: true,
      timeout: 120_000,
    },
    {
      // finding-admin 管理端, :3001
      command: 'npm --prefix ../finding-admin run dev',
      url: 'http://localhost:3001',
      reuseExistingServer: true,
      timeout: 120_000,
    },
  ],
});
