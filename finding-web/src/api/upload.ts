import request from './request';
import type { ApiResponse } from '../types/common';
import { compressImage } from '../utils/image';

export const uploadApi = {
  /** 上传单张图片(自动压缩)，返回图片 URL */
  uploadImage: async (file: File) => {
    const compressed = await compressImage(file);
    const formData = new FormData();
    formData.append('file', compressed);
    return request.post<ApiResponse<string>>('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};
