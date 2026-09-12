import { create } from 'zustand';

export type PreviewType = 'image' | 'video';

export interface PreviewItem {
  src: string;
  type: PreviewType;
}

export type PreviewInput = string | PreviewItem;

/** 归一化入参:字符串按图片处理;过滤空 src */
const normalize = (input: PreviewInput | PreviewInput[]): PreviewItem[] =>
  (Array.isArray(input) ? input : [input])
    .map((item) => (typeof item === 'string' ? { src: item, type: 'image' as const } : item))
    .filter((item) => !!item.src);

const clampIndex = (index: number, length: number) =>
  length === 0 ? 0 : Math.min(Math.max(index, 0), length - 1);

interface PreviewState {
  items: PreviewItem[];
  index: number;
  open: (input: PreviewInput | PreviewInput[], index?: number) => void;
  close: () => void;
  next: () => void;
  prev: () => void;
}

/**
 * 全局图片/视频预览状态。由 ImagePreviewHost 单实例渲染遮罩,
 * 各处通过 utils/preview 的 helper 调用,无需各自维护 state。
 */
export const usePreviewStore = create<PreviewState>((set) => ({
  items: [],
  index: 0,
  open: (input, index = 0) => {
    const items = normalize(input);
    if (items.length === 0) return; // 空分组不打开遮罩
    set({ items, index: clampIndex(index, items.length) });
  },
  close: () => set({ items: [], index: 0 }),
  // 单图时 next/prev 为空操作;多图环绕
  next: () => set((s) => (s.items.length < 2 ? s : { index: (s.index + 1) % s.items.length })),
  prev: () => set((s) => (s.items.length < 2 ? s : { index: (s.index - 1 + s.items.length) % s.items.length })),
}));
