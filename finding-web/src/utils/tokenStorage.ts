/**
 * 认证令牌单一存取点:所有 token 读写统一走这里,便于审计与集中清理。
 * 避免各处直接操作 localStorage 造成清理遗漏(登出/刷新/多标签页)。
 */
const ACCESS_KEY = 'accessToken';
const REFRESH_KEY = 'refreshToken';

/**
 * 解码 JWT 负载段(JSON)。
 * JWT 负载是 base64url(可能含 -/_ 且去掉 = 填充),浏览器 `atob` 只认标准 base64,
 * 直接调用会抛错导致「有效 token 被误判为过期而强制登出」。此处先转换再解码。
 */
export function decodeJwtPayload(token: string): unknown {
  const segment = token.split('.')[1];
  let base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  while (base64.length % 4 !== 0) base64 += '=';
  const binary = atob(base64);
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
  return JSON.parse(new TextDecoder().decode(bytes));
}

function isValid(token: string | null): boolean {
  if (!token) return false;
  try {
    const payload = decodeJwtPayload(token) as { exp?: number };
    if (!payload.exp) return false;
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

export const tokenStorage = {
  getAccess: (): string | null => localStorage.getItem(ACCESS_KEY),
  getRefresh: (): string | null => localStorage.getItem(REFRESH_KEY),
  set: (access: string, refresh: string) => {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  setAccess: (access: string) => localStorage.setItem(ACCESS_KEY, access),
  /** 读取并校验 access token;过期则整体清除并返回 null */
  getValidAccess: (): string | null => {
    const token = localStorage.getItem(ACCESS_KEY);
    if (isValid(token)) return token;
    tokenStorage.clear();
    return null;
  },
  clear: () => {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};
