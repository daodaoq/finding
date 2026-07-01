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
  createdAt: string;
  updatedAt: string;
}

export interface MateCategory {
  code: string;
  name: string;
  icon: string;
}
