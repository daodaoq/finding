import request from './request';
import type { ApiResponse, PageResult } from '../types/common';

export interface LoveGuide {
  id: number; title: string; subtitle: string; content: string; category: string;
  authorName?: string; createdAt?: string;
}

export const loveGuideApi = {
  list: (page = 1, size = 30) => request.get<ApiResponse<PageResult<LoveGuide>>>('/love-guides', { params: { page, size } }),
  create: (data: Pick<LoveGuide, 'title' | 'subtitle' | 'content' | 'category'>) => request.post<ApiResponse<LoveGuide>>('/love-guides', data),
};
