export interface User {
  id: number;
  nickname: string;
  avatar: string;
  gender: number;
  school: string;
  signature: string;
  city: string;
  realNameVerified: number;
  followerCount: number;
  followingCount: number;
  postCount: number;
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
  smsCode: string;
  password: string;
  nickname: string;
  school?: string;
  gender?: number;
}
