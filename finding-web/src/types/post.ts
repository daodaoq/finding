export interface Post {
  id: number;
  userId: number;
  content: string;
  images: string[];
  location: string;
  city: string;
  category?: string;
  categoryDesc?: string;
  tags?: string[];
  latitude: number;
  longitude: number;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  shareCount: number;
  isHot: number;
  isTop: number;
  visibility?: number;
  reviewStatus?: number;
  reviewReason?: string;
  author: import('./user').User | null;
  isLiked: boolean;
  isFavorited?: boolean;
  createdAt: string;
  updatedAt: string;
}
