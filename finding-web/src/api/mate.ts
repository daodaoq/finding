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

  /** 我加入的搭子，status: 空=全部, 1=进行中, 2=已结束 */
  myJoined: (page = 1, size = 20, status?: number) =>
    request.get<ApiResponse<PageResult<Mate>>>('/mates/my-joined', { params: { page, size, status } }),

  /** 我的全部申请记录(含待审核/已通过/被拒) */
  myApplications: (page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<MateApplication>>>('/mates/my-applications', { params: { page, size } }),

  /** 退出搭子 */
  leave: (id: number) =>
    request.delete<ApiResponse<null>>(`/mates/${id}/leave`),
};

/** 我的搭子申请记录 */
export interface MateApplication {
  invitationId: number;
  applicationStatus: number;   // 0=待审核 1=已通过 2=被拒
  message?: string;
  applyTime: string;
  title: string;
  category?: string;
  location?: string;
  activityTime?: string;
  invitationStatus: number;    // 邀约状态 0=已取消 1=进行中 2=已结束
  authorNickname?: string;
}
