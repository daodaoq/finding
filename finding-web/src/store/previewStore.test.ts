import { describe, it, expect, beforeEach } from 'vitest';
import { usePreviewStore } from './previewStore';

const s = () => usePreviewStore.getState();

describe('previewStore', () => {
  beforeEach(() => s().close());

  it('open(string) 归一化为单项', () => {
    s().open('a.png');
    expect(s().items).toEqual([{ src: 'a.png', type: 'image' }]);
    expect(s().index).toBe(0);
  });

  it('open(images, index) 记录整组与起始下标', () => {
    s().open(['a', 'b', 'c'], 2);
    expect(s().items).toHaveLength(3);
    expect(s().index).toBe(2);
  });

  it('next / prev 环绕', () => {
    s().open(['a', 'b', 'c'], 2);
    s().next();
    expect(s().index).toBe(0); // 末 → 首
    s().prev();
    expect(s().index).toBe(2); // 首 → 末
  });

  it('单图 next / prev 是空操作', () => {
    s().open(['only']);
    s().next();
    expect(s().index).toBe(0);
    s().prev();
    expect(s().index).toBe(0);
  });

  it('空分组不打开', () => {
    s().open([]);
    expect(s().items).toEqual([]);
  });

  it('下标越界被夹紧', () => {
    s().open(['a', 'b'], 99);
    expect(s().index).toBe(1);
  });

  it('close 重置 items 与 index', () => {
    s().open(['a', 'b'], 1);
    s().close();
    expect(s().items).toEqual([]);
    expect(s().index).toBe(0);
  });

  it('视频项保留 type', () => {
    s().open([{ src: 'v.mp4', type: 'video' }]);
    expect(s().items[0].type).toBe('video');
  });
});
