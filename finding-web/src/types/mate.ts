export interface Mate {
  id: number;
  userId: number;
  category: string;
  categoryDesc: string;
  title: string;
  description: string;
  activityTime: string;
  location: string;
  latitude: number;
  longitude: number;
  maxParticipants: number;
  currentParticipants: number;
  isAnonymous: number;
  status: number;
  author: import('./user').User | null;
  distanceKm: number;
  hasJoined: boolean;
  isFull: boolean;
  isExpired?: boolean;
  remainingSlots?: number;
  /** 当前用户报名状态:0=待审核 1=已通过 2=已拒绝 3=已退出 4=候补;未报名为 null */
  myApplicationStatus?: number | null;
  reviewStatus?: number;
  reviewReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface MateCategory {
  code: string;
  name: string;
}
