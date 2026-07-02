import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '../types/common';
import { useAuthStore, tryRefreshToken } from '../store/authStore';
import { showToast } from '../components/Toast';

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

let isRefreshing = false;
let refreshPromise: Promise<string | null> | null = null;

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken');
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
        useAuthStore.getState().logout();
      }
      if (res.code === 2003 || res.code === 2004 || res.code === 2005) {
        showToast(res.message || '请先完成学生认证');
      }
      return Promise.reject(new Error(res.message || 'Request failed'));
    }
    return response;
  },
  async (error: AxiosError) => {
    const status = error.response?.status;
    if (status === 401) {
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
        useAuthStore.getState().setToken(newToken);
        // 重试原请求
        const config = error.config as InternalAxiosRequestConfig;
        config.headers.Authorization = `Bearer ${newToken}`;
        return request(config);
      }
      // 刷新失败，踢出
      useAuthStore.getState().logout();
    } else if (status === 403) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  }
);

export default request;
