import { useEffect, useState } from 'react';
import type { Banner } from '../types/message';
import './BannerCarousel.css';

interface Props {
  banners: Banner[];
}

export default function BannerCarousel({ banners }: Props) {
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    if (banners.length <= 1) return;
    const timer = setInterval(() => {
      setCurrent((c) => (c + 1) % banners.length);
    }, 3000);
    return () => clearInterval(timer);
  }, [banners.length]);

  if (!banners.length) return null;

  return (
    <div className="banner-carousel">
      <div
        className="banner-track"
        style={{ transform: `translateX(-${current * 100}%)` }}
      >
        {banners.map((b, i) => (
          <div
            key={b.id || i}
            className="banner-slide"
            style={{ backgroundImage: `url(${b.imageUrl})` }}
          >
            <span className="banner-title">{b.title}</span>
          </div>
        ))}
      </div>
      <div className="banner-dots">
        {banners.map((_, i) => (
          <span key={i} className={`dot ${i === current ? 'active' : ''}`} />
        ))}
      </div>
    </div>
  );
}
