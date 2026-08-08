import { create } from 'zustand';

interface InfoShareState {
  /** 待处理的互换申请弹窗数据(null=不显示) */
  prompt: { shareId: number; fromUserId: number; fromNickname: string } | null;
  setPrompt: (p: InfoShareState['prompt']) => void;
  clearPrompt: () => void;
  /** 互换状态变更版本号 —— 聊天页/主页据此重新拉取状态 */
  version: number;
  bump: () => void;
}

export const useInfoShareStore = create<InfoShareState>((set) => ({
  prompt: null,
  setPrompt: (p) => set({ prompt: p }),
  clearPrompt: () => set({ prompt: null }),
  version: 0,
  bump: () => set((s) => ({ version: s.version + 1 })),
}));
