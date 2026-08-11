/** 相识卡片展示项配置(0=隐藏 1=显示) */
export interface UserCardConfig {
  showPhoto: number;
  showNickname: number;
  showGender: number;
  showSchool: number;
  showCity: number;
  showDistance: number;
  showSignature: number;
  showMatchReasons: number;
  showLastOnline: number;
}

export interface BridgeRecommendUser {
  userId: number;
  nickname: string;
  avatar: string;
  gender: number;
  school: string;
  signature: string;
  city: string;
  /** 距离km(无定位或隐藏时为 null) */
  distanceKm?: number;
  lastLoginAt: string;
  isLiked: boolean;
  mutualFriends: number;
  /** 匹配理由,如「同校」「已认证」「兴趣相投」 */
  matchReasons?: string[];
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
