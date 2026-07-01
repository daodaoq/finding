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

  submitVerification: (data: FormData) =>
    request.post<ApiResponse<null>>('/auth/verify', data),
};
