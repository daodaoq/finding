import { marked } from 'marked';
import DOMPurify from 'dompurify';

// 单换行 → <br>,便于公告类文案直接换行
marked.setOptions({ gfm: true, breaks: true });

/** 将 Markdown 文本渲染为已消毒的 HTML(防 XSS) */
export function renderMarkdown(text: string): string {
  if (!text) return '';
  const raw = marked.parse(text) as string;
  return DOMPurify.sanitize(raw);
}
