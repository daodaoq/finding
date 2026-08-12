import { useCallback, useEffect, useRef, useState } from 'react';
import { showToast } from '../components/Toast';
import type { ChatApply } from '../types/bridge';
import type { PageResult } from '../types/common';

const PAGE_SIZE = 20;

/** 申请状态筛选 Tab(0待通过/1已通过/2已拒绝/3已撤回/4已过期) */
export const APPLY_STATUS_TABS = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待通过' },
  { key: '1', label: '已通过' },
  { key: '2', label: '已拒绝' },
  { key: '3', label: '已撤回' },
  { key: '4', label: '已过期' },
] as const;

type ApplyFetcher = (
  page: number,
  size: number,
  status?: number
) => Promise<{ data: { data: PageResult<ChatApply> } }>;

/**
 * 聊天申请列表共用逻辑(我收到的/我发出的):
 * 状态 Tab 筛选 + 分页加载 + 加载更多。加载失败统一透出服务端文案。
 * 操作方(通过/拒绝/撤回)通过 setApplies 做乐观更新。
 */
export function useApplyList(fetcher: ApplyFetcher) {
  const [applies, setApplies] = useState<ChatApply[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);
  const [filter, setFilter] = useState('all');
  const fetcherRef = useRef(fetcher);
  fetcherRef.current = fetcher;

  const loadPage = useCallback(async (targetPage: number, append = false) => {
    if (append) setLoadingMore(true);
    else setLoading(true);
    try {
      const status = filter === 'all' ? undefined : Number(filter);
      const res = await fetcherRef.current(targetPage, PAGE_SIZE, status);
      const data = res.data.data;
      setApplies((prev) => (append ? [...prev, ...data.records] : data.records));
      setHasMore(data.hasMore);
      setPage(data.page);
    } catch (e) {
      // 服务端业务原因(冷却/拉黑/过期等)直接透出
      showToast((e as Error)?.message || '加载申请列表失败');
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, [filter]);

  // 筛选变化 → 重置并加载第一页
  useEffect(() => {
    loadPage(1);
  }, [filter, loadPage]);

  const handleTabChange = useCallback((key: string) => {
    setFilter((prev) => (prev === key ? prev : key));
  }, []);

  return {
    applies, setApplies, loading, loadingMore, page, hasMore, filter,
    handleTabChange, loadPage,
  };
}
