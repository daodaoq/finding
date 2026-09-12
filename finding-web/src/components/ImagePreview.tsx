import { useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { usePreviewStore } from '../store/previewStore';
import './ImagePreview.css';

/** 全局图片/视频预览遮罩。单实例,挂载于 MainLayout。 */
export default function ImagePreviewHost() {
  const items = usePreviewStore((s) => s.items);
  const index = usePreviewStore((s) => s.index);
  const close = usePreviewStore((s) => s.close);
  const next = usePreviewStore((s) => s.next);
  const prev = usePreviewStore((s) => s.prev);
  const { pathname } = useLocation();
  const start = useRef({ x: 0, y: 0 });

  const open = items.length > 0;
  const multiple = items.length > 1;
  const current = items[index];

  // 路由切换兜底关闭(用 pathname:桌面聊天切会话只变 search 参数,不应关闭预览)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { if (open) close(); }, [pathname]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') close();
      else if (e.key === 'ArrowRight') next();
      else if (e.key === 'ArrowLeft') prev();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, close, next, prev]);

  // 预览期间锁背景滚动
  useEffect(() => {
    if (!open) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = previous; };
  }, [open]);

  if (!open || !current) return null;

  const onTouchStart = (e: React.TouchEvent) => {
    start.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };
  };
  const onTouchEnd = (e: React.TouchEvent) => {
    const dx = e.changedTouches[0].clientX - start.current.x;
    const dy = e.changedTouches[0].clientY - start.current.y;
    // 横向位移足够大且明显大于纵向才切图,避免误触
    if (Math.abs(dx) > 40 && Math.abs(dx) > Math.abs(dy)) (dx < 0 ? next : prev)();
  };

  return (
    <div className="image-preview-overlay" onClick={close} onTouchStart={onTouchStart} onTouchEnd={onTouchEnd}>
      {current.type === 'video' ? (
        <video
          className="image-preview-img"
          src={current.src}
          controls
          autoPlay
          playsInline
          onClick={(e) => e.stopPropagation()}
        />
      ) : (
        <img className="image-preview-img" src={current.src} alt="" onClick={(e) => e.stopPropagation()} />
      )}

      {multiple && (
        <>
          <button
            className="image-preview-nav prev"
            aria-label="上一张"
            onClick={(e) => { e.stopPropagation(); prev(); }}
          >‹</button>
          <button
            className="image-preview-nav next"
            aria-label="下一张"
            onClick={(e) => { e.stopPropagation(); next(); }}
          >›</button>
          <div className="image-preview-counter">{index + 1} / {items.length}</div>
        </>
      )}

      <button className="image-preview-close" aria-label="关闭" onClick={(e) => { e.stopPropagation(); close(); }}>✕</button>
    </div>
  );
}
