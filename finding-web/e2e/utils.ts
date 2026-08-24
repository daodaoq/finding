import type { Page } from '@playwright/test';

/** 测试账号(见 finding-server/seed-test-data.sql) */
export const PHONE = '13096120690';
export const PASSWORD = '12345678';

/** 登录:密码登录成功后回到首页 */
export async function login(page: Page) {
  await page.goto('/login');
  await page.fill('input[type="tel"]', PHONE);
  await page.fill('input[type="password"]', PASSWORD);
  await page.click('.submit-btn');
  await page.waitForURL('/', { timeout: 10000 });
  // 关闭启动时弹出的「系统公告」弹窗(覆盖全屏会挡后续点击)
  await dismissAnnouncement(page);
}

/** 若系统公告弹窗弹出则关闭;无弹窗时快速跳过 */
export async function dismissAnnouncement(page: Page) {
  const btn = page.locator('.confirm-btn.primary');
  try {
    await btn.waitFor({ state: 'visible', timeout: 3000 });
    await btn.click();
    await btn.waitFor({ state: 'detached', timeout: 3000 });
  } catch {
    // 未弹出公告弹窗
  }
}
