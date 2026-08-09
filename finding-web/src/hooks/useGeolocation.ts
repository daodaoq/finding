import { useCallback, useEffect, useState } from 'react';

interface GeoState {
  lat?: number;
  lng?: number;
  status: 'idle' | 'loading' | 'success' | 'error';
}

/**
 * 基于浏览器 Geolocation API 获取定位。
 * - 需要用户授权;生产环境需 HTTPS(localhost 开发可用)
 * - 失败(拒绝/超时/不支持)时 status=error,静默降级,不影响功能
 */
export function useGeolocation(enabled = true) {
  const [geo, setGeo] = useState<GeoState>({ status: 'idle' });

  const request = useCallback(() => {
    if (!('geolocation' in navigator)) {
      setGeo({ status: 'error' });
      return;
    }
    setGeo({ status: 'loading' });
    navigator.geolocation.getCurrentPosition(
      (pos) => setGeo({
        lat: pos.coords.latitude,
        lng: pos.coords.longitude,
        status: 'success',
      }),
      () => setGeo({ status: 'error' }),
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 600000 },
    );
  }, []);

  useEffect(() => {
    if (enabled) request();
  }, [enabled, request]);

  return { lat: geo.lat, lng: geo.lng, status: geo.status, request };
}
