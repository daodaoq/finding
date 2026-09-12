import { test, expect } from '@playwright/test';
import { login } from './utils';

/**
 * 桌面端(≥768px)布局冒烟:顶栏/底栏切换、消息双栏聊天。
 * 依赖后端在线与种子账号(见 playwright.config.ts)。
 */
test.describe('桌面端布局', () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test('顶栏显示、底栏隐藏', async ({ page }) => {
    await login(page);
    await expect(page.locator('.top-nav')).toBeVisible();
    await expect(page.locator('.bottom-nav')).toBeHidden();
  });

  test('首页右栏:顶栏搜索 + 公告板模块', async ({ page }) => {
    await login(page);
    await page.goto('/');
    await expect(page.locator('.top-nav-search input')).toBeVisible({ timeout: 8000 });
    const rail = page.locator('.home-rail');
    await expect(rail).toBeVisible({ timeout: 8000 });
    await expect(rail.locator('.rail-card', { hasText: '热榜' })).toBeVisible();
  });

  test('图片预览遮罩铺满视口(不被 1100px 容器困住)', async ({ page }) => {
    await login(page);
    await page.goto('/');
    const thumb = page.locator('.post-image-item').first();
    if (await thumb.count() === 0) test.skip(true, '当前无带图动态');
    await thumb.click();
    const overlay = page.locator('.image-preview-overlay');
    await expect(overlay).toBeVisible({ timeout: 8000 });
    const box = await overlay.boundingBox();
    expect(box!.width).toBe(1280);
    await page.keyboard.press('Escape');
    await expect(overlay).toBeHidden();
  });

  test('消息双栏:左会话栏 + 空态占位,点开会话后仍在', async ({ page }) => {
    await login(page);
    await page.goto('/messages');
    await expect(page.locator('.msg-side-pane')).toBeVisible({ timeout: 8000 });
    await expect(page.locator('.chat-empty-placeholder')).toBeVisible();

    // 点击首个会话 → 右栏出现聊天输入框,左栏保持可见(不整页跳转)
    await page.locator('.msg-side-pane .chat-conv-item').first().click();
    const input = page.locator('input[placeholder="输入消息..."]');
    await expect(input).toBeVisible({ timeout: 8000 });
    await expect(page.locator('.msg-side-pane')).toBeVisible();

    // 发送消息 → 气泡出现且仍处于双栏结构内
    await input.fill(`桌面冒烟 ${Date.now()}`);
    await input.press('Enter');
    await expect(page.locator('.chat-bubble').last()).toContainText('桌面冒烟');
    await expect(page.locator('.msg-side-pane')).toBeVisible();
  });

  test('深链 /messages/chat?userId=2 在桌面双栏下可用', async ({ page }) => {
    await login(page);
    await page.goto('/messages/chat?userId=2');
    await expect(page.locator('.msg-side-pane')).toBeVisible({ timeout: 8000 });
    const input = page.locator('input[placeholder="输入消息..."]');
    await expect(input).toBeVisible({ timeout: 8000 });
    await expect(page.locator('.chat-bubble').first()).toBeVisible();
  });

  test('社区信息流单列:一行一个卡片', async ({ page }) => {
    await login(page);
    await page.goto('/');
    const list = page.locator('.home-post-list');
    await expect(list).toBeVisible({ timeout: 8000 });
    // 非 grid 布局(移动端同款块级堆叠),且整体收窄居中
    const display = await list.evaluate((el) => getComputedStyle(el).display);
    expect(display).not.toBe('grid');
    const cards = page.locator('.home-post-list .post-card');
    if (await cards.count() >= 2) {
      const first = await cards.nth(0).boundingBox();
      const second = await cards.nth(1).boundingBox();
      expect(second!.y).toBeGreaterThanOrEqual(first!.y + first!.height! - 1);
    }
  });
});
