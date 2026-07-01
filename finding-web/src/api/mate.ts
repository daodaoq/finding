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
};
