import { useRef, useState } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import type { Post } from '../types/post';
import PostShareCard from '../components/PostShareCard';
import { SHARE_CARD_WIDTH, canCopyImageToClipboard, copyImageToClipboard, dataUrlToBlob, generateCardImage } from '../utils/postShare';
import { showToast } from '../components/Toast';

const HOST_ID = 'post-share-card-host';

/**
 * 帖子分享卡片:把帖子快照渲染成 PNG。
 * - HTTPS 安全上下文:直接把图片写入剪贴板;
 * - 否则(HTTP)降级:返回预览图,由调用方弹窗引导长按保存。
 */
export function usePostShare() {
  const [preview, setPreview] = useState<string | null>(null);
  const rootRef = useRef<Root | null>(null);

  const cleanup = () => {
    rootRef.current?.unmount();
    rootRef.current = null;
    document.getElementById(HOST_ID)?.remove();
  };

  const share = async (post: Post): Promise<void> => {
    // 离屏容器渲染卡片快照(屏幕外固定宽度,截图后即移除)
    const host = document.createElement('div');
    host.id = HOST_ID;
    host.style.cssText = `position:fixed;left:-9999px;top:0;width:${SHARE_CARD_WIDTH}px;z-index:-1;`;
    document.body.appendChild(host);
    const root = createRoot(host);
    rootRef.current = root;
    root.render(<PostShareCard post={post} />);

    try {
      const dataUrl = await generateCardImage(host);
      if (canCopyImageToClipboard()) {
        const copied = await copyImageToClipboard(dataUrlToBlob(dataUrl));
        if (copied) {
          showToast('卡片已复制，去粘贴吧');
          return;
        }
      }
      // 降级:展示预览,引导长按保存
      setPreview(dataUrl);
    } catch {
      showToast('生成分享卡片失败，请稍后重试');
    } finally {
      cleanup();
    }
  };

  return { preview, setPreview, share };
}
