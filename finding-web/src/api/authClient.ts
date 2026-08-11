import axios from 'axios';

/**
 * 不带刷新拦截器的裸客户端,专用于 token 刷新等认证请求。
 * 主请求用 request.ts(Axios,带刷新重试);刷新请求用本实例,避免递归重试。
 */
export const authClient = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});
