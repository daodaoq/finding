import request from './request';
import type { ApiResponse } from '../types/common';
import { compressImage } from '../utils/image';

export const uploadApi = {
  /** 上传单张图片(自动压缩)，返回图片 URL；onProgress 用于显示上传进度(0-100) */
  uploadImage: async (file: File, onProgress?: (percent: number) => void) => {
    const compressed = await compressImage(file);
    const formData = new FormData();
    formData.append('file', compressed);
    return request.post<ApiResponse<string>>('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
        ? (e: any) => {
            if (e.total) onProgress(Math.round((e.loaded / e.total) * 100));
          }
        : undefined,
    });
  },
};
