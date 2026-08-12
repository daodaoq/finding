import { toPng } from 'html-to-image';

/** 卡片快照容器的渲染宽度(px),固定比例下高度由内容撑开 */
export const SHARE_CARD_WIDTH = 420;

/**
 * 当前环境能否把图片写入剪贴板。
 * 异步剪贴板 API(navigator.clipboard.write + ClipboardItem)要求 HTTPS 安全上下文,
 * HTTP 下该能力不存在,需走"弹窗 + 长按保存"降级。
 */
export function canCopyImageToClipboard(): boolean {
  return typeof window !== 'undefined'
    && window.isSecureContext === true
    && 'clipboard' in navigator
    && 'ClipboardItem' in window
    && typeof navigator.clipboard.write === 'function';
}

/** 渲染卡片节点为 PNG dataURL(先等字体加载完,避免文字错位) */
export async function generateCardImage(node: HTMLElement): Promise<string> {
  if (document.fonts && document.fonts.ready) {
    await document.fonts.ready;
  }
  return toPng(node, {
    pixelRatio: 2,
    backgroundColor: '#ffffff',
    cacheBust: true,
  });
}

/** PNG dataURL -> Blob(供 ClipboardItem / 下载使用) */
export function dataUrlToBlob(dataUrl: string): Blob {
  const [head, base64] = dataUrl.split(',');
  const mime = /data:(.*?);/.exec(head)?.[1] || 'image/png';
  const bin = atob(base64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return new Blob([bytes], { type: mime });
}

/** 尝试把图片写入剪贴板,成功返回 true;不支持或失败返回 false(走降级) */
export async function copyImageToClipboard(blob: Blob): Promise<boolean> {
  if (!canCopyImageToClipboard()) return false;
  try {
    await navigator.clipboard.write([new ClipboardItem({ 'image/png': blob })]);
    return true;
  } catch {
    return false;
  }
}
