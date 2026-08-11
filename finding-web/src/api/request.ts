import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '../types/common';
import { tokenStorage } from '../utils/tokenStorage';
import { useAuthStore, tryRefreshToken } from '../store/authStore';
import { showToast } from '../components/Toast';

// 标记已被 401 重试过的请求,避免刷新后再失败时无限递归
declare module 'axios' {
  interface InternalAxiosRequestConfig {
    _retry?: boolean;
  }
}

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

let isRefreshing = false;
let refreshPromise: Promise<string | null> | null = null;

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStorage.getAccess();
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse;
    if (res.code !== 200) {
      if (res.code === 1001 || res.code === 1003 || res.code === 1004) {
        tokenStorage.clear();
        useAuthStore.getState().logout();
      }
      if (res.code === 2003 || res.code === 2004 || res.code === 2005) {
        showToast(res.message || '请先完成学生认证');
      }
      // 违禁词拦截:直接展示服务端给出的具体违禁词提示
      if (res.code === 9010) {
        showToast(res.message || '内容包含违禁词');
      }
      return Promise.reject(new Error(res.message || 'Request failed'));
    }
    return response;
  },
  async (error: AxiosError) => {
    const status = error.response?.status;
    const config = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    if (status === 401 && config && !config._retry) {
      // 尝试 token 刷新
      if (!isRefreshing) {
        isRefreshing = true;
        refreshPromise = tryRefreshToken().finally(() => {
          isRefreshing = false;
          refreshPromise = null;
        });
      }
      const newToken = refreshPromise ? await refreshPromise : null;
      if (newToken) {
        config._retry = true; // 已重试过一次,再次 401 不再重试
        useAuthStore.getState().setToken(newToken);
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${newToken}`;
        return request(config);
      }
      // 刷新失败:统一清理并登出一次
      tokenStorage.clear();
      useAuthStore.getState().logout();
    } else if (status === 403) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  }
);

export default request;
