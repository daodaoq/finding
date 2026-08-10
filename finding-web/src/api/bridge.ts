import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { BridgeRecommendUser, ChatApply } from '../types/bridge';
import type { InfoShareStatus } from '../types/resume';

export const bridgeApi = {
  /** 分页获取推荐用户列表 */
  recommend: (params: { page?: number; size?: number; lat?: number; lng?: number }) =>
    request.get<ApiResponse<PageResult<BridgeRecommendUser>>>('/bridge/recommend', { params }),

  /** 发送聊天申请 */
  apply: (toUserId: number, remark?: string) =>
    request.post<ApiResponse<null>>('/bridge/apply', { toUserId, remark }),

  /** 我发出的申请列表 */
  sentApplies: (page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<ChatApply>>>('/bridge/apply/sent', { params: { page, size } }),

  /** 我收到的申请列表 */
  receivedApplies: (page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<ChatApply>>>('/bridge/apply/received', { params: { page, size } }),

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
  infoShareStatus: (userId: number) =>
    request.get<ApiResponse<InfoShareStatus>>('/bridge/info-share/status', {
      params: { userId },
    }),
};
