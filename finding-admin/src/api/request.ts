import axios from 'axios';
import { message } from 'antd';

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    // res 可能是 Spring Security 返回的非标准格式（如 403 页面）
    if (res && typeof res.code !== 'undefined' && res.code !== 200) {
      if (res.code === 1001 || res.code === 1003) {
        localStorage.removeItem('adminToken');
        window.location.href = '/admin/login';
        return Promise.reject(new Error(res.message));
      }
      message.error(res.message || '请求失败');
      return Promise.reject(new Error(res.message));
    }
    return response;
  },
  (error) => {
    const status = error.response?.status;
    if (status === 401 || status === 403) {
      localStorage.removeItem('adminToken');
      message.error('登录已过期，请重新登录');
      window.location.href = '/admin/login';
      return Promise.reject(error);
    }
    message.error('网络错误，请稍后重试');
    return Promise.reject(error);
  }
);

export default request;
