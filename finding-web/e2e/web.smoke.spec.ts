import { test, expect } from '@playwright/test';
import { login } from './utils';

/**
 * 用户端冒烟:登录 / 发布动态 / 搭子列表 / 聊天发消息 / 信息互换。
 * 依赖后端在线与种子账号(见 playwright.config.ts)。
 */

test.describe('用户端冒烟', () => {
  test('设置备注后昵称显示备注(用完即清)', async ({ page }) => {
    await login(page);
    await page.goto('/user/2');
    // 种子账号 1 与用户 2 不是同一人 → 显示操作区(含「设置备注」)
    const remarkBtn = page.locator('.up-remark-btn');
    await expect(remarkBtn).toBeVisible({ timeout: 8000 });

    const original = (await page.locator('.up-name').textContent())?.trim() || '';
    const remark = `备注${Date.now() % 100000}`;   // 限 20 字内
    await remarkBtn.click();
    await page.fill('.up-remark-input', remark);
    await page.locator('.up-greeting-send', { hasText: '保存' }).click();

    // 服务端按备注覆盖昵称,页面重拉后昵称位即为备注
    await expect(page.locator('.up-name')).toHaveText(remark, { timeout: 8000 });

    // 清理:清除备注后恢复真实昵称(避免污染开发库、影响其他用例)
    await page.locator('.up-remark-btn').click();
    await page.locator('.up-greeting-cancel', { hasText: '清除' }).click();
    await expect(page.locator('.up-name')).toHaveText(original, { timeout: 8000 });
  });


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
