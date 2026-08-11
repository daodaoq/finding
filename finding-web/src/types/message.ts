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
  pinned?: boolean;
  muted?: boolean;
}

/** 陌生人打招呼消息 */
export interface StrangerMessage {
  id: number;
  otherUserId: number;
  otherNickname: string;
  otherAvatar: string;
  content: string;
  /** sent=我发出的待确认 received=我收到的待确认 */
  direction: 'sent' | 'received';
  createdAt: string;
}

/** 会话设置(聊天信息页) */
export interface ChatSettings {
  roomId: number;
  pinned: boolean;
  muted: boolean;
  background: string | null;
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
  matchReasons?: string[];
}

export interface Banner {
  id: number;
  title: string;
  imageUrl: string;
  linkUrl: string;
}
