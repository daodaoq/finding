import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { BridgeRecommendUser, ChatApply } from '../types/bridge';

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

  /** 处理聊天申请（通过/拒绝） */
  handleApply: (id: number, approve: boolean) =>
    request.put<ApiResponse<null>>(`/bridge/apply/${id}/handle`, { status: approve ? 1 : 2 }),
};
