import { useCallback, useEffect, useRef, useState } from 'react';
import type { PageResult } from '../types/common';

export interface UseInfiniteListOptions<T, P extends unknown[]> {
  /** 每次拉取一页数据，返回 records 与 hasMore */
  fetcher: (page: number, ...args: P) => Promise<Pick<PageResult<T>, 'records' | 'hasMore'>>;
  /** 除页码外的固定参数，透传给 fetcher */
  args?: P;
  /** 依赖变化时自动重置列表并重新加载第一页（如筛选/排序条件） */
  deps?: ReadonlyArray<unknown>;
  /** 每页数量 */
  size?: number;
  /** 初始页码 */
  initialPage?: number;
  /** 拉取失败回调 */
  onError?: (e: unknown) => void;
}

export interface UseInfiniteListResult<T> {
  items: T[];
  /** 当前已加载的页码 */
  page: number;
  loading: boolean;
  hasMore: boolean;
  error: unknown;
  /** 直接替换列表（用于点赞/加入后的乐观更新） */
  setItems: React.Dispatch<React.SetStateAction<T[]>>;
  /** 加载下一页（加载按钮用） */
  loadMore: () => Promise<void>;
  /** 重置并重新加载第一页（刷新用） */
  reset: () => Promise<void>;
  /** 滚动容器接近底部时自动加载下一页 */
  onScroll: (e: React.UIEvent<HTMLElement>) => void;
}

/**
 * 统一「分页 + 滚动加载」逻辑。
 * 用法：
 *   const { items, loading, hasMore, setItems, loadMore, reset, onScroll } =
 *     useInfiniteList<Mate, [string, string]>({
 *       fetcher: (page, category, sortBy) => fetchMates(page, category, sortBy),
 *       args: [category, sortBy],
 *       deps: [category, sortBy],
 *       onError: () => showToast('加载失败'),
 *     });
 */
export function useInfiniteList<T, P extends unknown[] = unknown[]>(
  options: UseInfiniteListOptions<T, P>
): UseInfiniteListResult<T> {
  const {
    fetcher,
    args = [] as unknown as P,
    deps = [],
    initialPage = 1,
    onError,
  } = options;

  const [items, setItems] = useState<T[]>([]);
  const [page, setPage] = useState(initialPage);
  const [loading, setLoading] = useState(true);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState<unknown>(null);

  // 用 ref 保存最新引用，避免因回调身份变化触发重复请求
  const fetcherRef = useRef(fetcher);
  fetcherRef.current = fetcher;
  const argsRef = useRef(args);
  argsRef.current = args;
  const onErrorRef = useRef(onError);
  onErrorRef.current = onError;
  const pageRef = useRef(page);
  pageRef.current = page;
  const hasMoreRef = useRef(hasMore);
  hasMoreRef.current = hasMore;
  const loadingRef = useRef(loading);
  loadingRef.current = loading;
  // 竞态防护：只接受最近一次请求的结果
  const requestIdRef = useRef(0);

  const load = useCallback(async (targetPage: number) => {
    const requestId = ++requestIdRef.current;
    loadingRef.current = true;
    setLoading(true);
    setError(null);
    try {
      const res = await fetcherRef.current(targetPage, ...argsRef.current);
      if (requestId !== requestIdRef.current) return; // 过期请求直接丢弃
      setItems((prev) =>
        targetPage === initialPage ? res.records : [...prev, ...res.records]
      );
      setPage(targetPage);
      setHasMore(res.hasMore);
    } catch (e) {
      if (requestId !== requestIdRef.current) return;
      setError(e);
      onErrorRef.current?.(e);
    } finally {
      if (requestId === requestIdRef.current) {
        loadingRef.current = false;
        setLoading(false);
      }
    }
  }, [initialPage]);

  // 条件（筛选/排序）变化 → 清空列表并加载第一页
  useEffect(() => {
    requestIdRef.current = 0; // 作废在途请求
    setItems([]);
    setHasMore(true);
    load(initialPage);
    // 依赖由调用方显式声明
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps]);

  const loadMore = useCallback(async () => {
    if (loadingRef.current || !hasMoreRef.current) return;
    await load(pageRef.current + 1);
  }, [load]);

  const reset = useCallback(async () => {
    requestIdRef.current = 0;
    setItems([]);
    setHasMore(true);
    await load(initialPage);
  }, [load, initialPage]);

  const onScroll = useCallback((e: React.UIEvent<HTMLElement>) => {
    const el = e.currentTarget;
    if (el.scrollHeight - el.scrollTop <= el.clientHeight + 100) {
      loadMore();
    }
  }, [loadMore]);

  return { items, page, loading, hasMore, error, setItems, loadMore, reset, onScroll };
}