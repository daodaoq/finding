import { useEffect, useRef } from 'react';

/** 鹊桥桌面键盘操作:← 跳过 / → 心动。仅在 enabled(桌面+登录+有候选)时监听;
    过滤输入框/修饰键/连发,命中键才 preventDefault。 */
export function useSwipeKeyboard(enabled: boolean, handlers: { onPass: () => void; onLike: () => void }) {
  const ref = useRef(handlers);
  ref.current = handlers;

  useEffect(() => {
    if (!enabled) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.repeat || e.metaKey || e.ctrlKey || e.altKey) return;
      const t = e.target as HTMLElement | null;
      if (t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA' || t.tagName === 'SELECT' || t.isContentEditable)) return;
      if (e.key === 'ArrowLeft') { e.preventDefault(); ref.current.onPass(); }
      if (e.key === 'ArrowRight') { e.preventDefault(); ref.current.onLike(); }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [enabled]);
}
