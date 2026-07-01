import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { Conversation } from '../types/message';

export const chatApi = {
  /** 会话列表 */
  listConversations: () =>
    request.get<ApiResponse<Conversation[]>>('/chat/conversations'),

  /** 创建或获取会话 */
  getOrCreateConversation: (targetUserId: number) =>
    request.post<ApiResponse<Conversation>>('/chat/conversations', null, {
      params: { targetUserId }
    }),

  /** 发送消息 */
  sendMessage: (data: { toUserId: number; content: string; messageType?: string }) =>
    request.post<ApiResponse<Conversation>>('/chat/send', data),

  /** 消息历史（游标分页） */
  getMessageHistory: (conversationId: number, lastId?: number, size = 20) =>
    request.get<ApiResponse<PageResult<any>>>(`/chat/conversations/${conversationId}/messages`, {
      params: { lastId, size }
    }),

  /** 标记已读 */
  markRead: (conversationId: number) =>
    request.put<ApiResponse<null>>(`/chat/conversations/${conversationId}/read`),
};
