export interface Message {
  id: number;
  fromUserId: number;
  fromUserNickname: string;
  fromUserAvatar: string;
  toUserId: number;
  type: string;
  typeDesc: string;
  content: string;
  relatedId: number;
  isRead: number;
  createdAt: string;
}

export interface Conversation {
  id: number;
  roomId: number;
  targetUserId: number;
  targetNickname: string;
  targetAvatar: string;
  lastMessage: string;
  lastMessageAt: string;
  unreadCount: number;
}

export interface HomeFeedUser {
  userId: number;
  nickname: string;
  avatar: string;
  gender: number;
  school: string;
  signature: string;
  city: string;
  distanceKm: number;
  lastLoginAt: string;
  isLiked: boolean;
  mutualFriends: number;
}

export interface Banner {
  id: number;
  title: string;
  imageUrl: string;
  linkUrl: string;
}
