import request from './request';
import type { ApiResponse } from '../types/common';
import type { UserResume, ResumeView } from '../types/resume';

export const resumeApi = {
  /** 获取我的情感简历(未填写返回 data=null) */
  getMine: () =>
    request.get<ApiResponse<UserResume | null>>('/resume/me'),

  /** 保存我的情感简历 */
  save: (data: Partial<UserResume>) =>
    request.put<ApiResponse<null>>('/resume/me', data),

  /** 查看他人情感简历(需已互换信息,否则返回锁定状态) */
  getOther: (userId: number, signal?: AbortSignal) =>
    request.get<ApiResponse<ResumeView>>(`/users/${userId}/resume`, { signal }),
};
