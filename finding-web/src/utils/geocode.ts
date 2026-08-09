/** 浏览器定位 + 逆向地理编码(坐标 → 城市名)。 */

/** 获取浏览器定位坐标(WGS84,需用户授权;生产环境需 HTTPS) */
export function getBrowserPosition(): Promise<{ lat: number; lng: number }> {
  return new Promise((resolve, reject) => {
    if (!('geolocation' in navigator)) {
      reject(new Error('当前浏览器不支持定位'));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      (err) => reject(new Error(err.message || '定位失败，请检查浏览器权限')),
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 600000 },
    );
  });
}

/**
 * 逆向地理编码:坐标 → 城市名(免密钥,BigDataCloud,中文)。
 * 如需接入高德/百度地图,替换此函数即可(它们需要 API key)。
 */
export async function reverseGeocodeCity(lat: number, lng: number): Promise<string> {
  try {
    const res = await fetch(
      `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lng}&localityLanguage=zh`,
    );
    const data = await res.json();
    return data.city || data.locality || data.principalSubdivision || '';
  } catch {
    return '';
  }
}

/** 一步到位:浏览器定位 → 城市名(失败返回空串) */
export async function locateCity(): Promise<string> {
  const pos = await getBrowserPosition();
  return reverseGeocodeCity(pos.lat, pos.lng);
}
