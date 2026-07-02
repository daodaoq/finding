import { create } from 'zustand';
import type { User } from '../types/user';

/** 解码 JWT 并检查是否过期 */
function isTokenValid(token: string | null): boolean {
  if (!token) return false;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (!payload.exp) return false;
    // exp 是秒级时间戳，乘以 1000 转毫秒
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

/** 获取有效的已存储 token，过期则自动清理 */
function getStoredToken(): string | null {
  const token = localStorage.getItem('accessToken');
  if (isTokenValid(token)) return token;
  // token 过期或无效，清理
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  return null;
}

interface AuthState {
  user: User | null;
  token: string | null;
  isLoggedIn: boolean;
  setAuth: (user: User, token: string) => void;
  setUser: (user: User) => void;
  logout: () => void;
}

const validToken = getStoredToken();

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: validToken,
  isLoggedIn: !!validToken,

  setAuth: (user, token) => {
    localStorage.setItem('accessToken', token);
    set({ user, token, isLoggedIn: true });
  },

  setUser: (user) => set({ user }),

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    set({ user: null, token: null, isLoggedIn: false });
  },
}));
