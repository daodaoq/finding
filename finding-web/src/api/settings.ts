import request from './request';
import type { ApiResponse } from '../types/common';

/** 用户全局设置 */
export interface UserSettings {
  userId: number;
  chatBg?: string | null;    // 全局默认聊天背景
  chatMuted: number;         // 全局默认免打扰 0/1
  friendAddMode: number;     // 加好友方式 0=所有人 1=需验证 2=不允许
  profileVisible: number;    // 主页可见性 1=所有人 2=仅已互换
  searchable: number;        // 可被搜索 0/1
}

export const settingsApi = {
  get: () =>
    request.get<ApiResponse<UserSettings>>('/user-settings'),

  update: (data: Partial<UserSettings>) =>
    request.put<ApiResponse<null>>('/user-settings', data),
};
