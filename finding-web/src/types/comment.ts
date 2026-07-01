export interface Comment {
  id: number;
  postId: number;
  userId: number;
  nickname: string;
  avatar: string;
  parentId: number | null;
  content: string;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
  replies?: Comment[];
  replyCount?: number;
}
