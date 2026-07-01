import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { Message, Conversation } from '../types/message';

export const messageApi = {
  list: (params?: { type?: string; page?: number; size?: number }) =>
    request.get<ApiResponse<PageResult<Message>>>('/messages', { params }),

  unreadCount: () =>
    request.get<ApiResponse<{ count: number }>>('/messages/unread-count'),

  markRead: (id: number) =>
    request.put<ApiResponse<null>>(`/messages/${id}/read`),

  markAllRead: (type?: string) =>
    request.put<ApiResponse<null>>('/messages/read-all', null, { params: { type } }),

  conversations: (page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<Conversation>>>('/messages/conversations', {
      params: { page, size }
    }),
};
