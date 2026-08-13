/** 相识卡片展示项配置(0=隐藏 1=显示) */
export interface UserCardConfig {
  showPhoto: number;
  showNickname: number;
  showAge: number;
  showGender: number;
  showSchool: number;
  showCity: number;
  showDistance: number;
  showVerified: number;
  showTargetType: number;
  showSignature: number;
  showMatchReasons: number;
  showLastOnline: number;
}

export interface BridgeRecommendUser {
  userId: number;
  nickname: string;
  avatar: string;
  gender: number;
  /** 年龄(有生日时计算,隐藏时为 null) */
  age?: number;
  school: string;
  signature: string;
  city: string;
  /** 距离km(无定位或隐藏时为 null) */
  distanceKm?: number;
  /** 是否已实名认证 0/1 */
  verified?: number;
  /** 交友目标 0=未设置 1=找对象 2=交朋友 */
  targetType?: number;
  lastLoginAt: string;
  /** 是否实时在线(Redis 心跳) */
  online?: boolean;
  isLiked: boolean;
  /** 是否已心动(双向 match 的喜欢) */
  liked?: boolean;
  mutualFriends: number;
  /** 匹配理由,如「同校」「已认证」「兴趣相投」 */
  matchReasons?: string[];
}

/** 心动/配对列表项 */
export interface MatchUser {
  userId: number;
  nickname: string;
  avatar: string;
  gender: number;
  school: string;
  signature: string;
  verified: number;
  targetType: number;
  time: string;
  isMatched: boolean;
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
