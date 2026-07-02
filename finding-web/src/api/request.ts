import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '../types/common';
import { useAuthStore } from '../store/authStore';
import { showToast } from '../components/Toast';

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor — attach JWT
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor — unwrap & handle errors
request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse;
    if (res.code !== 200) {
      // 认证相关错误：清除登录态
      if (res.code === 1001 || res.code === 1003 || res.code === 1004) {
        useAuthStore.getState().logout();
      }
      // 学生认证相关错误：弹提示引导用户去认证
      if (res.code === 2003 || res.code === 2004 || res.code === 2005) {
        showToast(res.message || '请先完成学生认证');
      }
      return Promise.reject(new Error(res.message || 'Request failed'));
    }
    return response;
  },
  (error: AxiosError) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  }
);

export default request;
