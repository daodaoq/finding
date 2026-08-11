import { describe, it, expect } from 'vitest';
import { renderMarkdown } from './markdown';

/**
 * P1-6 富文本安全策略显式测试。
 * AnnouncementModal 将 renderMarkdown 的输出直接注入 dangerouslySetInnerHTML,
 * 因此该函数的最终 HTML 必须始终经过 DOMPurify 消毒。这里对报告的
 * 脚本标签 / 事件属性 / javascript: URL / SVG payload 四类载荷做回归保护。
 */
describe('renderMarkdown XSS 消毒', () => {
  it('删除 <script> 标签', () => {
    const html = renderMarkdown('<script>alert(1)</script>hello');
    expect(html).not.toMatch(/<script/i);
    expect(html).toContain('hello');
  });

  it('删除元素事件属性(onclick/onerror)', () => {
    const html = renderMarkdown('<img src=x onerror="alert(1)"><div onclick="alert(2)">hi</div>');
    expect(html).not.toMatch(/onerror/i);
    expect(html).not.toMatch(/onclick/i);
    expect(html).toContain('hi');
  });

  it('剥离 javascript: URL 协议', () => {
    const html = renderMarkdown('[点我](javascript:alert(1))');
    expect(html).not.toMatch(/javascript:/i);
    // 链接文本仍保留
    expect(html).toContain('点我');
  });

  it('消毒 SVG payload 中的脚本', () => {
    const html = renderMarkdown('<svg><script>alert(1)</script></svg>');
    expect(html).not.toMatch(/<script/i);
  });

  it('保留合法 Markdown 与安全的 https 链接', () => {
    const html = renderMarkdown('**加粗** 与 [链接](https://example.com/a?b=1)');
    expect(html).toContain('<strong>加粗</strong>');
    expect(html).toContain('href="https://example.com/a?b=1"');
    expect(html).toContain('链接');
  });

  it('空输入返回空串', () => {
    expect(renderMarkdown('')).toBe('');
    expect(renderMarkdown(undefined as unknown as string)).toBe('');
  });
});
