import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { HomeFeedUser, Banner } from '../types/message';

/** 系统公告 */
export interface Announcement {
  id: number;
  title: string;
  content: string;
  type?: number;    // 1=普通公告(弹窗) 2=永久展示(顶部横条)
  status?: number;  // 1=展示中 0=已下架
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

  /** 当前生效的永久展示公告(顶部悬浮横条),无则 data=null */
  permanentAnnouncement: () =>
    request.get<ApiResponse<Announcement | null>>('/home/permanent-announcements'),
};
