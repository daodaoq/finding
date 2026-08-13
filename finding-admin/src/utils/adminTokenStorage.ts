/**
 * 管理端认证令牌单一存取点。
 * 所有 adminToken 读写统一走这里,避免各处直接操作 localStorage 造成清理遗漏
 * (登出 / 401 过期 / 多页面并发请求)。
 */
const ADMIN_TOKEN_KEY = 'adminToken';

/** JWT 负载段为 base64url,`atob` 只认标准 base64,先转换再解码,避免有效 token 被误判过期 */
function decodePayload(token: string): unknown {
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
    const payload = decodePayload(token) as { exp?: number };
    if (!payload.exp) return false;
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

export const adminTokenStorage = {
  get: (): string | null => localStorage.getItem(ADMIN_TOKEN_KEY),
  set: (token: string) => localStorage.setItem(ADMIN_TOKEN_KEY, token),
  /** 读取并校验 token;已过期则整体清除并返回 null */
  getValid: (): string | null => {
    const token = localStorage.getItem(ADMIN_TOKEN_KEY);
    if (isValid(token)) return token;
    adminTokenStorage.clear();
    return null;
  },
  clear: () => localStorage.removeItem(ADMIN_TOKEN_KEY),
};
