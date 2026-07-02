import request from './request';
import type { ApiResponse } from '../types/common';

export const uploadApi = {
  /** 上传单张图片，返回图片 URL */
  uploadImage: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return request.post<ApiResponse<string>>('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};
