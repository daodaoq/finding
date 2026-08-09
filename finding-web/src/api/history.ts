import request from './request';
import type { ApiResponse, PageResult } from '../types/common';

/** 一条浏览记录 */
export interface HistoryRecord {
  targetType: 'post' | 'user';
  targetId: number;
  title: string;
  image?: string;
  subtitle?: string;
  createdAt: string;
}

export const historyApi = {
  /** 记录一次浏览(post/user) */
  record: (targetType: 'post' | 'user', targetId: number) =>
    request.post<ApiResponse<null>>('/history', { targetType, targetId }),

  /** 我最近浏览列表 */
  list: (page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<HistoryRecord>>>('/history', { params: { page, size } }),
};
