import { create } from 'zustand';

interface BridgeState {
  /** 我收到的待处理(未处理)申请数 —— 用于「鹊桥」「情书」角标 */
  pendingCount: number;
  setPendingCount: (count: number) => void;
}

export const useBridgeStore = create<BridgeState>((set) => ({
  pendingCount: 0,
  setPendingCount: (count) => set({ pendingCount: count }),
}));
