export interface BridgeRecommendUser {
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

export interface ChatApply {
  id: number;
  fromUserId: number;
  fromUserNickname: string;
  fromUserAvatar: string;
  toUserId: number;
  toUserNickname: string;
  toUserAvatar: string;
  status: number;         // 0=pending, 1=approved, 2=rejected
  statusDesc: string;
  remark: string;
  applyTime: string;
  handleTime: string;
  conversationId: number;
}
