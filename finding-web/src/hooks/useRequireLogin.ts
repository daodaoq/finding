import { useState, useCallback } from 'react';
import { useAuthStore } from '../store/authStore';

/**
 * 需要登录才能执行操作的 Hook。
 * 用法: const { requireLogin } = useRequireLogin();
 *       <button onClick={() => requireLogin(() => doSomething())}>操作</button>
 *
 * 如果已登录 → 直接执行 callback
 * 如果未登录 → 弹出登录弹窗，登录成功后执行 callback
 */
export function useRequireLogin() {
  const [showLogin, setShowLogin] = useState(false);
  const [pendingCallback, setPendingCallback] = useState<(() => void) | null>(null);
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);

  const requireLogin = useCallback((callback: () => void) => {
    if (isLoggedIn) {
      callback();
    } else {
      setPendingCallback(() => callback);
      setShowLogin(true);
    }
  }, [isLoggedIn]);

  const handleLoginSuccess = useCallback(() => {
    setShowLogin(false);
    pendingCallback?.();
    setPendingCallback(null);
  }, [pendingCallback]);

  const handleClose = useCallback(() => {
    setShowLogin(false);
    setPendingCallback(null);
  }, []);

  return {
    showLogin,
    requireLogin,
    handleLoginSuccess,
    handleClose,
    isLoggedIn,
  };
}
