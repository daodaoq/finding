import { create } from 'zustand';
import type { User } from '../types/user';

function isTokenValid(token: string | null): boolean {
  if (!token) return false;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (!payload.exp) return false;
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

function getStoredToken(): string | null {
  const token = localStorage.getItem('accessToken');
  if (isTokenValid(token)) return token;
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  return null;
}

/** 使用 refreshToken 刷新 accessToken，失败返回 null */
export async function tryRefreshToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) return null;
  try {
    const res = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    const json = await res.json();
    if (json.code === 200 && json.data) {
      const newToken = json.data;
      localStorage.setItem('accessToken', newToken);
      return newToken;
    }
    return null;
  } catch {
    return null;
  }
}

interface AuthState {
  user: User | null;
  token: string | null;
  isLoggedIn: boolean;
  setAuth: (user: User, token: string) => void;
  setUser: (user: User) => void;
  setToken: (token: string) => void;
  logout: () => Promise<void>;
}

const validToken = getStoredToken();

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  token: validToken,
  isLoggedIn: !!validToken,

  setAuth: (user, token) => {
    localStorage.setItem('accessToken', token);
    set({ user, token, isLoggedIn: true });
  },

  setUser: (user) => set({ user }),

  setToken: (token) => {
    localStorage.setItem('accessToken', token);
    set({ token });
  },

  logout: async () => {
    // 通知服务端销毁 token
    const token = get().token;
    if (token) {
      try {
        await fetch('/api/v1/auth/logout', {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
        });
      } catch { /* 网络错误不阻塞登出 */ }
    }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    set({ user: null, token: null, isLoggedIn: false });
  },
}));
