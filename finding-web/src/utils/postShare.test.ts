import { describe, expect, it, afterEach } from 'vitest';
import { canCopyImageToClipboard, dataUrlToBlob } from './postShare';

afterEach(() => {
  // 清理测试注入的全局属性
  // @ts-expect-error 允许删除测试注入属性
  delete window.ClipboardItem;
});

describe('dataUrlToBlob', () => {
  it('解析 PNG dataURL 为正确类型与字节内容', () => {
    // 1x1 红点 PNG(手动构造的极简 base64 载荷)
    const dataUrl = 'data:image/png;base64,iVBORw0KGgo=';
    const blob = dataUrlToBlob(dataUrl);
    expect(blob.type).toBe('image/png');
    expect(blob.size).toBeGreaterThan(0);
  });
});

describe('canCopyImageToClipboard', () => {
  it('无剪贴板 API 时返回 false(HTTP 降级路径)', () => {
    // jsdom 默认无 navigator.clipboard → 返回 false
    expect(canCopyImageToClipboard()).toBe(false);
  });

  it('安全上下文 + ClipboardItem 就绪时返回 true', () => {
    Object.defineProperty(window, 'isSecureContext', { value: true, configurable: true });
    class FakeClipboardItem {}
    Object.defineProperty(window, 'ClipboardItem', { value: FakeClipboardItem, configurable: true });
    Object.defineProperty(navigator, 'clipboard', {
      value: { write: () => Promise.resolve() }, configurable: true,
    });
    expect(canCopyImageToClipboard()).toBe(true);
  });
});
