export interface User {
  id: number;
  nickname: string;
  avatar: string;
  /** 个人资料卡背景图，仅由本人设置和编辑。 */
  profileBackground?: string;
  gender: number;
  school: string;
  signature: string;
  city: string;
  realNameVerified: number;
  targetType?: number;
  followerCount: number;
  followingCount: number;
  postCount: number;
  /** 互关(好友)数量 */
  mutualCount?: number;
  isFollowed: boolean;
  lastLoginAt: string;
  createdAt: string;
}

export interface LoginParams {
  phone: string;
  loginType: 'password' | 'sms';
  password?: string;
  smsCode?: string;
}

export interface RegisterParams {
  phone: string;
  captchaKey: string;
  captchaCode: string;
  password: string;
  nickname: string;
  school?: string;
  gender?: number;
}
