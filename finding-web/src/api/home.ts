import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { HomeFeedUser, Banner } from '../types/message';

/** 系统公告 */
export interface Announcement {
  id: number;
  title: string;
  content: string;
  createdBy?: number;
  createdAt: string;
}

export const homeApi = {
  feed: (params: { page?: number; size?: number; lat?: number; lng?: number }) =>
    request.get<ApiResponse<PageResult<HomeFeedUser>>>('/home/feed', { params }),

  banners: () =>
    request.get<ApiResponse<Banner[]>>('/home/banners'),

  /** 获取指定ID之后的系统公告(启动时补拉全部未读) */
  announcements: (afterId: number) =>
    request.get<ApiResponse<Announcement[]>>('/home/announcements', { params: { afterId } }),
};
