import { Navigate, Outlet, useLocation } from 'react-router-dom';

/**
 * 后台路由鉴权守卫:无 token 立即跳转登录页,并保存原目标地址,登录后回跳。
 * 前端守卫只是交互层防线,最终权限以后端 admin API 的 RBAC 校验为准。
 */
export default function RequireAdminAuth() {
  const location = useLocation();
  const token = localStorage.getItem('adminToken');
  if (!token) {
    const from = location.pathname + location.search;
    return <Navigate to="/login" state={{ from }} replace />;
  }
  return <Outlet />;
}
