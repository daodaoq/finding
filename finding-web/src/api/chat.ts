import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { Conversation, ChatSettings } from '../types/message';

export const chatApi = {
  /** 会话列表 */
  listConversations: () =>
    request.get<ApiResponse<Conversation[]>>('/chat/conversations'),

  /** 创建或获取会话 */
  getOrCreateConversation: (targetUserId: number) =>
    request.post<ApiResponse<Conversation>>('/chat/conversations', null, {
      params: { targetUserId }
    }),

  /** 发送消息(以 roomId 指定会话,接收者由服务端从房间成员推导) */
  sendMessage: (data: { roomId: number; content: string; messageType?: string }) =>
    request.post<ApiResponse<Conversation>>('/chat/send', data),

  /** 消息历史（id=room_id） */
  getMessageHistory: (roomId: number, lastId?: number, size = 50) =>
    request.get<ApiResponse<PageResult<any>>>(`/chat/conversations/${roomId}/messages`, {
      params: { lastId, size }
    }),

  /** 标记已读 */
  markRead: (conversationId: number) =>
    request.put<ApiResponse<null>>(`/chat/conversations/${conversationId}/read`),

  /** 获取会话设置(置顶/免打扰/聊天背景) */
  getSettings: (roomId: number) =>
    request.get<ApiResponse<ChatSettings>>(`/chat/conversations/${roomId}/settings`),

  /** 更新会话设置 */
  updateSettings: (roomId: number, data: { pinned?: boolean; muted?: boolean; background?: string }) =>
    request.put<ApiResponse<null>>(`/chat/conversations/${roomId}/settings`, data),

  /** 搜索会话内聊天记录 */
  searchMessages: (roomId: number, keyword: string, size = 50) =>
    request.get<ApiResponse<PageResult<any>>>(`/chat/conversations/${roomId}/messages/search`, {
      params: { keyword, size },
    }),

  /** 清空会话聊天记录 */
  clearMessages: (roomId: number) =>
    request.delete<ApiResponse<null>>(`/chat/conversations/${roomId}/messages`),

  /** 投诉用户 */
  reportUser: (data: { targetUserId: number; roomId?: number; reason: string }) =>
    request.post<ApiResponse<null>>('/chat/report', data),

  /** 撤回自己发送的消息(2分钟内) */
  recallMessage: (messageId: number) =>
    request.post<ApiResponse<null>>(`/chat/messages/${messageId}/recall`),
};
