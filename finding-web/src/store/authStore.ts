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

// 多标签页同步:监听其他标签页对 localStorage 的 token 变更(登出/刷新/登录),
// 保持各标签页登录态一致,避免一个标签页登出后另一个仍处于登录态。
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    // tokenStorage.clear() 会 removeItem('accessToken') 触发 key='accessToken' 且 newValue=null
    if (event.key !== 'accessToken' && event.key !== null) return;
    const token = tokenStorage.getValidAccess();
    if (!token) {
      // 其他标签页登出或令牌失效 → 本标签页同步登出(不重复通知服务端)
      if (useAuthStore.getState().isLoggedIn) {
        useAuthStore.setState({ user: null, token: null, isLoggedIn: false });
      }
    } else if (!useAuthStore.getState().isLoggedIn) {
      // 其他标签页刷新/登录得到新 token → 本标签页同步登录态(仅标记,用户信息待重新拉取)
      useAuthStore.setState({ token, isLoggedIn: true });
    }
  });
}
