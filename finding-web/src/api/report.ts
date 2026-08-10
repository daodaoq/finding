import request from './request';
import type { ApiResponse } from '../types/common';

export interface MyReport {
  id: number;
  targetType: string;
  targetId: number;
  reason: string;
  contentSnapshot?: string;
  status: number;       // 0=待处理 1=已处理 2=驳回
  handleNote?: string;
  handleTime?: string;
  createdAt: string;
}

export const reportApi = {
  /** 统一投诉:message/post/comment/user/resume */
  report: (data: { targetType: string; targetId: number; reason: string; roomId?: number; evidence?: string[] }) =>
    request.post<ApiResponse<null>>('/report', data),

  /** 我提交的投诉记录(含处理状态/意见) */
  myReports: () =>
    request.get<ApiResponse<MyReport[]>>('/report/mine'),
};
