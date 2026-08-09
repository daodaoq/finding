import request from './request';
import type { ApiResponse } from '../types/common';

export const reportApi = {
  /** 统一投诉:message/post/comment/user/resume */
  report: (data: { targetType: string; targetId: number; reason: string; roomId?: number }) =>
    request.post<ApiResponse<null>>('/report', data),
};
