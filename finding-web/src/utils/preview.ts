import type { MouseEvent } from 'react';
import { usePreviewStore } from '../store/previewStore';

/** 打开图片预览;传数组即多图分组,index 为起始下标 */
export function openImagePreview(src: string | string[], index = 0) {
  usePreviewStore.getState().open(src, index);
}

/** 打开视频预览(单项,不显示箭头与计数器) */
export function openVideoPreview(src: string) {
  usePreviewStore.getState().open([{ src, type: 'video' }]);
}

/**
 * 头像 / 缩略图 / 背景图点击处理器工厂。
 * - 有图:拦截冒泡(避免触发外层卡片/行的导航)并打开预览;
 * - 无图:返回 undefined → 外层容器原有交互(导航、选中)完全不变。
 */
export function previewHandler(src?: string | null) {
  if (!src) return undefined;
  return (e: MouseEvent) => {
    e.stopPropagation();
    openImagePreview(src);
  };
}

/** 多图分组点击:整组进 store,预览内可翻到组内全部图片 */
export function galleryHandler(images: string[], index: number) {
  return (e: MouseEvent) => {
    e.stopPropagation();
    openImagePreview(images, index);
  };
}
