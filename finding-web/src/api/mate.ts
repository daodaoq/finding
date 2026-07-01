import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { Mate, MateCategory } from '../types/mate';

export const mateApi = {
  list: (params: Record<string, unknown>) =>
    request.get<ApiResponse<PageResult<Mate>>>('/mates', { params }),

  detail: (id: number) =>
    request.get<ApiResponse<Mate>>(`/mates/${id}`),

  create: (data: Record<string, unknown>) =>
    request.post<ApiResponse<Mate>>('/mates', data),

  join: (id: number, message?: string) =>
    request.post<ApiResponse<null>>(`/mates/${id}/join`, null, { params: { message } }),

  categories: () =>
    request.get<ApiResponse<MateCategory[]>>('/mates/categories'),

  /** 我发布的邀约 */
  myInvitations: (page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<Mate>>>('/mates/my', { params: { page, size } }),

  /** 我加入的搭子 */
  myJoined: (page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<Mate>>>('/mates/my-joined', { params: { page, size } }),

  /** 退出搭子 */
  leave: (id: number) =>
    request.delete<ApiResponse<null>>(`/mates/${id}/leave`),
};
