import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { BridgeRecommendUser, ChatApply, UserCardConfig } from '../types/bridge';
import type { InfoShareStatus } from '../types/resume';

export interface UserMatchPreference {
  preferGender: number;   // 0=不限 1=男 2=女
  minAge: number;
  maxAge: number;
  maxDistanceKm: number;
  onlyVerified: number;   // 0=否 1=是
  preferCity?: string;
  preferTargetType: number; // 0=不限 1=找对象 2=交朋友
  minCompleteness: number;  // 0-10,0=不限
}

export const bridgeApi = {
  /** 分页获取推荐用户列表 */
  recommend: (params: { page?: number; size?: number; lat?: number; lng?: number }) =>
    request.get<ApiResponse<PageResult<BridgeRecommendUser>>>('/bridge/recommend', { params }),

  /** 发送聊天申请 */
  apply: (toUserId: number, remark?: string) =>
    request.post<ApiResponse<null>>('/bridge/apply', { toUserId, remark }),

  /** 我发出的申请列表(status 可空,按状态筛选) */
  sentApplies: (page = 1, size = 20, status?: number) =>
    request.get<ApiResponse<PageResult<ChatApply>>>('/bridge/apply/sent', { params: { page, size, status } }),

  /** 我收到的申请列表(status 可空,按状态筛选) */
  receivedApplies: (page = 1, size = 20, status?: number) =>
    request.get<ApiResponse<PageResult<ChatApply>>>('/bridge/apply/received', { params: { page, size, status } }),

  /** 我收到的待处理申请数（情书入口角标） */
  receivedPendingCount: () =>
    request.get<ApiResponse<{ count: number }>>('/bridge/apply/received/pending-count'),

  /** 处理聊天申请（通过/拒绝） */
  handleApply: (id: number, approve: boolean) =>
    request.put<ApiResponse<null>>(`/bridge/apply/${id}/handle`, { status: approve ? 1 : 2 }),

  /** 撤回我发出的待处理申请 */
  withdrawApply: (id: number) =>
    request.post<ApiResponse<null>>(`/bridge/apply/${id}/withdraw`),

  /** 发起信息互换申请 */
  infoShareRequest: (toUserId: number) =>
    request.post<ApiResponse<null>>('/bridge/info-share/request', { toUserId }),

  /** 处理信息互换申请（1=同意, 2=拒绝） */
  infoShareHandle: (id: number, status: 1 | 2) =>
    request.put<ApiResponse<null>>(`/bridge/info-share/${id}/handle`, { status }),

  /** 查询我与对方的信息互换状态 */
  infoShareStatus: (userId: number, signal?: AbortSignal) =>
    request.get<ApiResponse<InfoShareStatus>>('/bridge/info-share/status', {
      params: { userId }, signal,
    }),

  /** 我的相亲交友偏好 */
  getPreference: () =>
    request.get<ApiResponse<UserMatchPreference>>('/bridge/preference'),

  /** 更新相亲交友偏好 */
  updatePreference: (data: Partial<UserMatchPreference>) =>
    request.put<ApiResponse<null>>('/bridge/preference', data),

  /** 对某候选「不感兴趣」:排除出推荐流 */
  skipUser: (userId: number) =>
    request.post<ApiResponse<null>>(`/bridge/recommend/${userId}/skip`),

  /** 我的相识卡片展示配置 */
  getCardConfig: () =>
    request.get<ApiResponse<UserCardConfig>>('/bridge/card-config'),

  /** 更新我的相识卡片展示配置 */
  updateCardConfig: (data: Partial<UserCardConfig>) =>
    request.put<ApiResponse<null>>('/bridge/card-config', data),

  /** 预览我的卡片(别人视角,按配置裁剪) */
  previewMyCard: () =>
    request.get<ApiResponse<BridgeRecommendUser>>('/bridge/card-config/preview'),
};
