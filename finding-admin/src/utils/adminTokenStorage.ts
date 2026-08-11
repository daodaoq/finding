/**
 * 管理端认证令牌单一存取点。
 * 所有 adminToken 读写统一走这里,避免各处直接操作 localStorage 造成清理遗漏
 * (登出 / 401 过期 / 多页面并发请求)。
 */
const ADMIN_TOKEN_KEY = 'adminToken';

function isValid(token: string | null): boolean {
  if (!token) return false;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
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
