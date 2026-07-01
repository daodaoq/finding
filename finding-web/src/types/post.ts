export interface Post {
  id: number;
  userId: number;
  content: string;
  images: string[];
  location: string;
  city: string;
  latitude: number;
  longitude: number;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  shareCount: number;
  isHot: number;
  isTop: number;
  author: import('./user').User | null;
  isLiked: boolean;
  createdAt: string;
  updatedAt: string;
}
