import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { HomeFeedUser, Banner } from '../types/message';

export const homeApi = {
  feed: (params: { page?: number; size?: number; lat?: number; lng?: number }) =>
    request.get<ApiResponse<PageResult<HomeFeedUser>>>('/home/feed', { params }),

  banners: () =>
    request.get<ApiResponse<Banner[]>>('/home/banners'),
};
