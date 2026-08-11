import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { ChatMessageDTO, Conversation, ChatSettings, StrangerMessage } from '../types/message';

export const chatApi = {
  /** 会话列表 */
  listConversations: () =>
    request.get<ApiResponse<Conversation[]>>('/chat/conversations'),

  /** 已隐藏会话列表(用于手动恢复) */
  listHiddenConversations: () =>
    request.get<ApiResponse<Conversation[]>>('/chat/conversations/hidden'),

  /** 发送陌生人打招呼消息(未建立会话前,同一对象仅一条) */
  sendStrangerMessage: (toUserId: number, content: string) =>
    request.post<ApiResponse<null>>('/chat/stranger/send', null, { params: { toUserId, content } }),

  /** 我的陌生人消息合集(我发出的待确认 + 我收到的待确认) */
  listStrangerMessages: () =>
    request.get<ApiResponse<StrangerMessage[]>>('/chat/stranger/messages'),

  /** 确认聊天:转为正式会话 */
  acceptStrangerMessage: (id: number) =>
    request.post<ApiResponse<null>>(`/chat/stranger/${id}/accept`),

  /** 与某用户的陌生人消息状态 */
  strangerStatus: (toUserId: number) =>
    request.get<ApiResponse<{ hasConversation: boolean; sent: boolean; received: boolean }>>('/chat/stranger/status', {
      params: { toUserId }
    }),

  /** 创建或获取会话 */
  getOrCreateConversation: (targetUserId: number) =>
    request.post<ApiResponse<Conversation>>('/chat/conversations', null, {
      params: { targetUserId }
    }),

  /** 发送消息(以 roomId 指定会话,返回真实消息回执;clientMessageId 用于弱网重试幂等;replyToMessageId 回复引用) */
  sendMessage: (data: { roomId: number; content: string; messageType?: string; clientMessageId?: string; replyToMessageId?: number }) =>
    request.post<ApiResponse<ChatMessageDTO>>('/chat/send', data),

  /** 消息历史（id=room_id） */
  getMessageHistory: (roomId: number, lastId?: number, size = 50) =>
    request.get<ApiResponse<PageResult<ChatMessageDTO>>>(`/chat/conversations/${roomId}/messages`, {
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
    request.get<ApiResponse<PageResult<ChatMessageDTO>>>(`/chat/conversations/${roomId}/messages/search`, {
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

  /** 隐藏/恢复会话(单侧,不删除房间;收到新消息自动恢复) */
  hideConversation: (roomId: number, hidden: boolean) =>
    request.put<ApiResponse<null>>(`/chat/conversations/${roomId}/hidden`, { hidden }),
};
