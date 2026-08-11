import request from './request';
import type { ApiResponse } from '../types/common';
import type { LoginParams, RegisterParams, User } from '../types/user';

/** 我的认证记录(仅本人可见) */
export interface MyVerification {
  id: number;
  userId: number;
  realName: string;
  studentId: string;
  school: string;
  idCardFront?: string;
  idCardBack?: string;
  studentCard?: string;
  status: number;          // 0=待审核 1=已通过 2=已拒绝
  reviewerId?: number;
  reviewComment?: string;
  createdAt: string;
}

export const authApi = {
  login: (data: LoginParams) =>
    request.post<ApiResponse<{ accessToken: string; refreshToken: string }>>('/auth/login', data),

  register: (data: RegisterParams) =>
    request.post<ApiResponse<null>>('/auth/register', data),

  sendCode: (phone: string, type: string) =>
    request.post<ApiResponse<null>>('/auth/send-code', { phone, type }),

  /** 获取图片验证码 */
  getCaptcha: () =>
    request.get<ApiResponse<{ captchaKey: string; captchaImage: string }>>('/auth/captcha'),

  getMe: () =>
    request.get<ApiResponse<User>>('/auth/me'),

  updateProfile: (data: Partial<User>) =>
    request.put<ApiResponse<null>>('/auth/profile', data),

  /** 获取我的认证记录(仅本人) */
  getMyVerification: () =>
    request.get<ApiResponse<MyVerification | null>>('/auth/verification'),

  /** 修改密码 */
  changePassword: (oldPassword: string, newPassword: string) =>
    request.post<ApiResponse<null>>('/auth/password', { oldPassword, newPassword }),

  /** 当前用户账号信息(仅本人,含手机号) */
  getAccount: () =>
    request.get<ApiResponse<{ phone: string }>>('/auth/account'),

  /** 注销账号(需输入密码二次确认) */
  deleteAccount: (password: string) =>
    request.post<ApiResponse<null>>('/auth/delete-account', { password }),

  submitVerification: (data: { realName: string; studentId: string; school: string; studentCard?: string }) => {
    const params = new URLSearchParams();
    params.append('realName', data.realName);
    params.append('studentId', data.studentId);
    params.append('school', data.school);
    if (data.studentCard) params.append('studentCard', data.studentCard);
    return request.post<ApiResponse<null>>('/auth/verify', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    });
  },
};
