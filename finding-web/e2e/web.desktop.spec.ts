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

  test('首页信息流在桌面呈现多列网格', async ({ page }) => {
    await login(page);
    await page.goto('/');
    const list = page.locator('.home-post-list');
    await expect(list).toBeVisible({ timeout: 8000 });
    const columns = await list.evaluate((el) => getComputedStyle(el).gridTemplateColumns.split(' ').length);
    expect(columns).toBeGreaterThan(1);
  });
});
