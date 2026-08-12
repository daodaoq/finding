import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';

/**
 * 用户端冒烟:登录 / 发布动态 / 搭子列表 / 聊天发消息 / 信息互换。
 * 依赖后端在线与种子账号(见 playwright.config.ts)。
 */
const PHONE = '13096120690';
const PASSWORD = '12345678';

/** 登录:密码登录成功后回到首页 */
async function login(page: Page) {
  await page.goto('/login');
  await page.fill('input[type="tel"]', PHONE);
  await page.fill('input[type="password"]', PASSWORD);
  await page.click('.submit-btn');
  await page.waitForURL('/', { timeout: 10000 });
  // 关闭启动时弹出的「系统公告」弹窗(覆盖全屏会挡后续点击)
  await dismissAnnouncement(page);
}

/** 若系统公告弹窗弹出则关闭;无弹窗时快速跳过 */
async function dismissAnnouncement(page: Page) {
  const btn = page.locator('.confirm-btn.primary');
  try {
    await btn.waitFor({ state: 'visible', timeout: 3000 });
    await btn.click();
    await btn.waitFor({ state: 'detached', timeout: 3000 });
  } catch {
    // 未弹出公告弹窗
  }
}

test.describe('用户端冒烟', () => {
  test('登录成功进入首页', async ({ page }) => {
    await login(page);
    await expect(page.locator('.bottom-nav, .home-page, .feed').first()).toBeVisible({ timeout: 8000 });
  });

  test('发布动态', async ({ page }) => {
    await login(page);
    await page.goto('/create-post');
    const textarea = page.locator('.cp-textarea');
    await expect(textarea).toBeVisible({ timeout: 8000 });
    await textarea.fill(`冒烟测试动态 ${Date.now()}`);
    const submit = page.locator('.cp-submit-btn');
    await expect(submit).toBeEnabled({ timeout: 8000 });
    await submit.click();
    // 发布成功 → handleSubmit 调用 navigate(-1) 离开创建页;失败则停留(toast 随导航销毁,不可作为信号)
    await page.waitForURL((url) => !url.pathname.includes('/create-post'), { timeout: 8000 });
  });

  test('搭子列表加载', async ({ page }) => {
    await login(page);
    await page.goto('/mate');
    // 列表有内容或空态(有数据即可判定请求链通)
    await expect(page.locator('.mate-card, .empty-state, .search-empty').first()).toBeVisible({ timeout: 8000 });
  });

  test('聊天发送消息(种子账号已有会话)', async ({ page }) => {
    await login(page);
    await page.goto('/messages/chat?userId=2');
    const input = page.locator('input[placeholder="输入消息..."]');
    await expect(input).toBeVisible({ timeout: 8000 });
    await input.fill(`冒烟消息 ${Date.now()}`);
    await input.press('Enter');
    await expect(page.locator('.chat-bubble').last()).toContainText('冒烟消息');
  });

  test('信息互换:聊天页展示已有互换状态', async ({ page }) => {
    await login(page);
    await page.goto('/messages/chat?userId=2');
    // 种子数据:用户 1 与 2 已有已通过的互换记录 → 显示「已互换信息」
    await expect(page.locator('.share-tag', { hasText: '已互换信息' })).toBeVisible({ timeout: 8000 });
  });
});
