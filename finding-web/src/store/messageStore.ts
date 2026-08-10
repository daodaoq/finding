import { create } from 'zustand';
import { messageApi } from '../api/message';
import { chatApi } from '../api/chat';
import { groupChatApi } from '../api/groupChat';

interface MessageState {
  unreadCount: number;
  setUnreadCount: (count: number) => void;
  incrementUnread: () => void;
  decrementUnread: (by?: number) => void;
  /** 汇总角标 = 互动通知未读 + 私聊未读 + 群聊未读 */
  refreshTotal: () => Promise<void>;
}

export const useMessageStore = create<MessageState>((set) => ({
  unreadCount: 0,
  setUnreadCount: (count) => set({ unreadCount: count }),
  incrementUnread: () => set((s) => ({ unreadCount: s.unreadCount + 1 })),
  decrementUnread: (by = 1) => set((s) => ({ unreadCount: Math.max(0, s.unreadCount - by) })),
  refreshTotal: async () => {
    try {
      const [msgRes, convRes, groupRes] = await Promise.all([
        messageApi.unreadCount(),
        chatApi.listConversations(),
        groupChatApi.listMyGroups(),
      ]);
      const notif = msgRes.data.data.count || 0;
      const priv = (convRes.data.data || []).reduce((s, c: any) => s + (c.unreadCount || 0), 0);
      const grp = (groupRes.data.data || []).reduce((s, g: any) => s + (g.unreadCount || 0), 0);
      set({ unreadCount: notif + priv + grp });
    } catch { /* 忽略 */ }
  },
}));
