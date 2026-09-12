import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { User } from '../types/user';

export interface ProfileCompleteness {
  score: number;
  filled: number;
  total: number;
  missing: string[];
}

export const userApi = {
  getProfile: (id: number, signal?: AbortSignal) =>
    request.get<ApiResponse<User>>(`/users/${id}`, { signal }),

  /** 我的资料完整度(含缺失项) */
  getCompleteness: () =>
    request.get<ApiResponse<ProfileCompleteness>>(`/users/me/completeness`),

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
  blockStatus: (id: number, signal?: AbortSignal) =>
    request.get<ApiResponse<{ blocked: boolean; blockedBy: boolean }>>(`/users/${id}/block-status`, { signal }),

  // ── 备注(私密别名):仅本人可见,设置后全站以备注替代对方昵称 ──

  /** 设置/修改我对某人的备注(最长 20 字) */
  setRemark: (id: number, remark: string) =>
    request.post<ApiResponse<null>>(`/users/${id}/remark`, { remark }),

  /** 清除我对某人的备注 */
  clearRemark: (id: number) =>
    request.delete<ApiResponse<null>>(`/users/${id}/remark`),

  /** 我对某人的备注;无备注返回空串 */
  getRemark: (id: number, signal?: AbortSignal) =>
    request.get<ApiResponse<string>>(`/users/${id}/remark`, { signal }),
};
