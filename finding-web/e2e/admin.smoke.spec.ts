import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';

/**
 * 管理端冒烟:未登录访问受保护页被重定向;管理员登录后进入数据面板/用户管理。
 * finding-admin dev server 在 :3001。管理员账号见 seed 数据(id=10)。
 */
const ADMIN_BASE = 'http://localhost:3001';
const ADMIN_PHONE = '13800000000';
const ADMIN_PASSWORD = '12345678';

async function adminLogin(page: Page) {
  await page.goto(`${ADMIN_BASE}/admin/login`);
  await page.fill('input[placeholder="管理员手机号"]', ADMIN_PHONE);
  await page.fill('input[placeholder="密码"]', ADMIN_PASSWORD);
  await page.click('button:has-text("登 录"), button:has-text("登录")');
  // 登录成功跳数据面板(默认 from=/dashboard)
  await expect(page).toHaveURL(/dashboard/, { timeout: 8000 });
}

test.describe('管理端冒烟', () => {
  test('未登录访问受保护页 → 重定向登录页', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/admin/users`);
    // RequireAdminAuth 无 token 立即跳登录页(SPA 内 navigate)
    await expect(page).toHaveURL(/\/admin\/login/, { timeout: 8000 });
  });

  test('管理员登录后进入数据面板', async ({ page }) => {
    await adminLogin(page);
    await expect(page.locator('.ant-layout, .ant-card').first()).toBeVisible({ timeout: 8000 });
  });

  test('用户管理表格加载', async ({ page }) => {
    await adminLogin(page);
    await page.goto(`${ADMIN_BASE}/admin/users`);
    await expect(page.locator('.ant-table-row').first()).toBeVisible({ timeout: 10000 });
  });
});
