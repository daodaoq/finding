import { message } from 'antd';
import type { NavigateFunction } from 'react-router-dom';
import { adminTokenStorage } from './adminTokenStorage';

/**
 * 在 React Router 上下文外(如 axios 拦截器)触发路由跳转的统一入口。
 * App 内挂载的组件在挂载时注册 navigate,保持 SPA 内跳转,避免整页刷新
 * (window.location.href 会丢失内存状态并重复渲染)。
 */
let navigator: NavigateFunction | null = null;
export const registerNavigator = (fn: NavigateFunction) => { navigator = fn; };
export const unregisterNavigator = () => { navigator = null; };
const navigateTo = (to: string, replace = true) => {
  if (navigator) navigator(to, { replace });
  else window.location.href = to; // 兜底:路由上下文未就绪时整页跳转
};

let loggingOut = false;

/**
 * 集中登出:清 token + 提示 + 跳转。
 * 并发 401 会多次触发,内部加锁保证只提示/跳转一次。
 */
export function logoutAdmin(reason = '登录已过期，请重新登录') {
  adminTokenStorage.clear();
  if (loggingOut) return;
  loggingOut = true;
  message.error(reason);
  navigateTo('/login');
  setTimeout(() => { loggingOut = false; }, 1500);
}
