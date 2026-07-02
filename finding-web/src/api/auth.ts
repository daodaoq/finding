import request from './request';
import type { ApiResponse } from '../types/common';
import type { LoginParams, RegisterParams, User } from '../types/user';

export const authApi = {
  login: (data: LoginParams) =>
    request.post<ApiResponse<{ accessToken: string; refreshToken: string }>>('/auth/login', data),

  register: (data: RegisterParams) =>
    request.post<ApiResponse<null>>('/auth/register', data),

  sendCode: (phone: string, type: string) =>
    request.post<ApiResponse<null>>('/auth/send-code', { phone, type }),

  getMe: () =>
    request.get<ApiResponse<User>>('/auth/me'),

  updateProfile: (data: Partial<User>) =>
    request.put<ApiResponse<null>>('/auth/profile', data),

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
