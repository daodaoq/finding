import { create } from 'zustand';
import type { User } from '../types/user';
import { tokenStorage } from '../utils/tokenStorage';
import { authClient } from '../api/authClient';

/** 使用 refreshToken 刷新 accessToken，失败返回 null（通过 authClient 避免递归重试） */
export async function tryRefreshToken(): Promise<string | null> {
  const refreshToken = tokenStorage.getRefresh();
  if (!refreshToken) return null;
  try {
    const res = await authClient.post('/auth/refresh', { refreshToken });
    const json = res.data;
    if (json.code === 200 && json.data) {
      tokenStorage.setAccess(json.data);
      return json.data;
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

const validToken = tokenStorage.getValidAccess();

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  token: validToken,
  isLoggedIn: !!validToken,

  setAuth: (user, token) => {
    tokenStorage.setAccess(token);
    set({ user, token, isLoggedIn: true });
  },

  setUser: (user) => set({ user }),

  setToken: (token) => {
    tokenStorage.setAccess(token);
    set({ token });
  },

  logout: async () => {
    // 通知服务端销毁 token(失败不阻塞登出)
    const token = get().token;
    if (token) {
      try {
        await authClient.post('/auth/logout', null, { headers: { Authorization: `Bearer ${token}` } });
      } catch { /* 网络错误不阻塞登出 */ }
    }
    tokenStorage.clear();
    set({ user: null, token: null, isLoggedIn: false });
  },
}));
