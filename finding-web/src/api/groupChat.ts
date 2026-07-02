import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { GroupChat, GroupMessage, InvitableUser } from '../types/groupChat';

export const groupChatApi = {
  listMyGroups: () =>
    request.get<ApiResponse<GroupChat[]>>('/chat/groups'),

  createGroup: (name: string, memberIds: number[]) =>
    request.post<ApiResponse<GroupChat>>('/chat/groups', { name, memberIds }),

  getGroupDetail: (id: number) =>
    request.get<ApiResponse<GroupChat>>(`/chat/groups/${id}`),

  getInvitableUsers: (id: number) =>
    request.get<ApiResponse<InvitableUser[]>>(`/chat/groups/${id}/invitable`),

  sendMessage: (groupId: number, content: string, messageType = 'text') =>
    request.post<ApiResponse<GroupMessage>>(`/chat/groups/${groupId}/send`, { content, messageType }),

  getMessageHistory: (groupId: number, page = 1, size = 50) =>
    request.get<ApiResponse<PageResult<GroupMessage>>>(`/chat/groups/${groupId}/messages`, { params: { page, size } }),

  addMembers: (groupId: number, userIds: number[]) =>
    request.post<ApiResponse<null>>(`/chat/groups/${groupId}/members`, { userIds }),

  removeMember: (groupId: number, userId: number) =>
    request.delete<ApiResponse<null>>(`/chat/groups/${groupId}/members/${userId}`),

  leaveOrDisband: (groupId: number) =>
    request.delete<ApiResponse<null>>(`/chat/groups/${groupId}`),
};
