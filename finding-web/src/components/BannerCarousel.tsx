import { useEffect, useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Banner } from '../types/message';
import './BannerCarousel.css';

interface Props {
  banners: Banner[];
}

export default function BannerCarousel({ banners }: Props) {
  const [current, setCurrent] = useState(0);
  const [dragging, setDragging] = useState(false);
  const startX = useRef(0);
  const offsetX = useRef(0);
  const timerRef = useRef<ReturnType<typeof setInterval>>();
  const navigate = useNavigate();

  const len = banners.length;

  const startTimer = useCallback(() => {
    stopTimer();
    if (len <= 1) return;
    timerRef.current = setInterval(() => setCurrent((c) => (c + 1) % len), 3000);
  }, [len]);

  const stopTimer = () => {
    if (timerRef.current) clearInterval(timerRef.current);
  };

  useEffect(() => {
    startTimer();
    return stopTimer;
  }, [startTimer]);

  const onStart = (clientX: number) => {
    stopTimer();
    startX.current = clientX;
    offsetX.current = 0;
    setDragging(true);
  };

  const onMove = (clientX: number) => {
    if (!dragging) return;
    offsetX.current = clientX - startX.current;
  };

  const onEnd = () => {
    setDragging(false);
    const threshold = 50;
    if (offsetX.current > threshold) {
      setCurrent((c) => (c - 1 + len) % len);
    } else if (offsetX.current < -threshold) {
      setCurrent((c) => (c + 1) % len);
    }
    offsetX.current = 0;
    startTimer();
  };

  if (!len) return null;

  // 单张不显示指示器
  const translateX = dragging
    ? `calc(-${current * 100}% + ${offsetX.current}px)`
    : `-${current * 100}%`;

  return (
    <div
      className="banner-carousel"
      onTouchStart={(e) => onStart(e.touches[0].clientX)}
      onTouchMove={(e) => onMove(e.touches[0].clientX)}
      onTouchEnd={onEnd}
      onMouseDown={(e) => { e.preventDefault(); onStart(e.clientX); }}
      onMouseMove={(e) => dragging && onMove(e.clientX)}
      onMouseUp={onEnd}
      onMouseLeave={() => dragging && onEnd()}
    >
      <div
        className="banner-track"
        style={{ transform: `translateX(${translateX})`, transition: dragging ? 'none' : 'transform 0.4s ease' }}
      >
        {banners.map((b, i) => (
          <div
            key={b.id || i}
            className="banner-slide"
            style={{ backgroundImage: `url(${b.imageUrl})` }}
            onClick={() => { if (b.linkUrl) navigate(b.linkUrl); }}
          >
            <span className="banner-title">{b.title}</span>
          </div>
        ))}
      </div>

      {len > 1 && (
        <div className="banner-dots">
          {banners.map((_, i) => (
            <span key={i} className={`dot ${i === current ? 'active' : ''}`}
              onClick={() => { setCurrent(i); startTimer(); }} />
          ))}
        </div>
      )}
    </div>
  );
}
