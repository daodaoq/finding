import type { CSSProperties } from 'react';

/** 聊天背景预设 —— key → CSS 背景样式 */
export const CHAT_BG_PRESETS: Record<string, CSSProperties> = {
  'grad-1': { background: 'linear-gradient(160deg, #fdfcfb 0%, #e2d1c3 100%)' },
  'grad-2': { background: 'linear-gradient(160deg, #a1c4fd 0%, #c2e9fb 100%)' },
  'grad-3': { background: 'linear-gradient(160deg, #fbc2eb 0%, #a6c1ee 100%)' },
  'grad-4': { background: 'linear-gradient(160deg, #f6d365 0%, #fda085 100%)' },
  'grad-5': { background: 'linear-gradient(160deg, #84fab0 0%, #8fd3f4 100%)' },
  'grad-6': { background: 'linear-gradient(160deg, #d299c2 0%, #fef9d7 100%)' },
};

/** 解析设置里的 background 值为聊天区域样式(图片 URL 或预设 key) */
export function resolveChatBg(bg?: string | null): CSSProperties | undefined {
  if (!bg) return undefined;
  // 上传的图片是相对路径(如 /api/v1/images/xxx)或外链 http(s),都按图片背景处理
  if (bg.startsWith('http') || bg.startsWith('/')) {
    return {
      backgroundImage: `url(${bg})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
    };
  }
  return CHAT_BG_PRESETS[bg] || undefined;
}
