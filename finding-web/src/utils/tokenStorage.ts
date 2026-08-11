/**
 * 认证令牌单一存取点:所有 token 读写统一走这里,便于审计与集中清理。
 * 避免各处直接操作 localStorage 造成清理遗漏(登出/刷新/多标签页)。
 */
const ACCESS_KEY = 'accessToken';
const REFRESH_KEY = 'refreshToken';

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
