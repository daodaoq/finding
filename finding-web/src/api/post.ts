import request from './request';
import type { ApiResponse, PageResult } from '../types/common';
import type { Post } from '../types/post';
import type { Comment } from '../types/comment';

export const postApi = {
  list: (params: { tab?: string; page?: number; size?: number; city?: string }) =>
    request.get<ApiResponse<PageResult<Post>>>('/posts', { params }),

  myPosts: (page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<Post>>>('/posts/my', { params: { page, size } }),

  detail: (id: number) =>
    request.get<ApiResponse<Post>>(`/posts/${id}`),

  create: (data: { content: string; images?: string[]; location?: string; city?: string }) =>
    request.post<ApiResponse<Post>>('/posts', data),

  myLikes: (page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<Post>>>('/posts/my-likes', { params: { page, size } }),

  delete: (id: number) =>
    request.delete<ApiResponse<null>>(`/posts/${id}`),

  like: (id: number) =>
    request.post<ApiResponse<null>>(`/posts/${id}/like`),

  getComments: (postId: number, page = 1, size = 20) =>
    request.get<ApiResponse<PageResult<Comment>>>(`/posts/${postId}/comments`, { params: { page, size } }),

  addComment: (postId: number, content: string, parentId?: number) =>
    request.post<ApiResponse<Comment>>(`/posts/${postId}/comments`, null, {
      params: { content, parentId }
    }),

  deleteComment: (postId: number, commentId: number) =>
    request.delete<ApiResponse<null>>(`/posts/${postId}/comments/${commentId}`),

  likeComment: (postId: number, commentId: number) =>
    request.post<ApiResponse<null>>(`/posts/${postId}/comments/${commentId}/like`),
};
