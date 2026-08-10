import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { User } from '../types/user';

export const userApi = {
  getProfile: (id: number) =>
    request.get<ApiResponse<User>>(`/users/${id}`),

  follow: (id: number) =>
    request.post<ApiResponse<null>>(`/users/${id}/follow`),

  unfollow: (id: number) =>
    request.delete<ApiResponse<null>>(`/users/${id}/follow`),

  getFollowers: (id: number, page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<User>>>(`/users/${id}/followers`, { params: { page, size } }),

  getFollowing: (id: number, page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<User>>>(`/users/${id}/following`, { params: { page, size } }),

  /** 互相关注列表 */
  getMutualFollows: (id: number, page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<User>>>(`/users/${id}/mutual-follows`, { params: { page, size } }),

  block: (id: number) =>
    request.post<ApiResponse<null>>(`/users/${id}/block`),

  unblock: (id: number) =>
    request.delete<ApiResponse<null>>(`/users/${id}/block`),

  /** blocked=我拉黑了对方, blockedBy=对方拉黑了我 */
  blockStatus: (id: number) =>
    request.get<ApiResponse<{ blocked: boolean; blockedBy: boolean }>>(`/users/${id}/block-status`),
};
