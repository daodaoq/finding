export interface GroupChat {
  id: number;
  name: string;
  avatar: string;
  ownerId: number;
  memberCount: number;
  announcement: string;
  lastMessage: string;
  lastMessageAt: string;
  createdAt: string;
  members?: GroupMember[];
}

export interface GroupMember {
  userId: number;
  nickname: string;
  avatar: string;
  role: number; // 0=member, 1=admin, 2=owner
}

export interface GroupMessage {
  id: number;
  groupId: number;
  fromUserId: number;
  fromUserNickname: string;
  fromUserAvatar: string;
  content: string;
  messageType: string;
  createdAt: string;
}

export interface InvitableUser {
  userId: number;
  nickname: string;
  avatar: string;
}
