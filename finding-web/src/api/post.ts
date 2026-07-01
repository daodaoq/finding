import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { Post } from '../types/post';

export const postApi = {
  list: (params: { tab?: string; page?: number; size?: number; city?: string }) =>
    request.get<ApiResponse<PageResult<Post>>>('/posts', { params }),

  detail: (id: number) =>
    request.get<ApiResponse<Post>>(`/posts/${id}`),

  create: (data: { content: string; images?: string[]; location?: string; city?: string }) =>
    request.post<ApiResponse<Post>>('/posts', data),

  delete: (id: number) =>
    request.delete<ApiResponse<null>>(`/posts/${id}`),

  like: (id: number) =>
    request.post<ApiResponse<null>>(`/posts/${id}/like`),

  addComment: (postId: number, content: string, parentId?: number) =>
    request.post<ApiResponse<Post>>(`/posts/${postId}/comments`, null, {
      params: { content, parentId }
    }),
};
