import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bridgeApi } from '../../api/bridge';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { showToast } from '../../components/Toast';
import AppIcon from '../../components/AppIcon';
import type { ChatApply } from '../../types/bridge';
import './subpage.css';

const PAGE_SIZE = 20;

/** 全部 + 各状态(0待通过/1已通过/2已拒绝/3已撤回/4已过期) */
const STATUS_TABS = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待通过' },
  { key: '1', label: '已通过' },
  { key: '2', label: '已拒绝' },
  { key: '3', label: '已撤回' },
  { key: '4', label: '已过期' },
] as const;

export default function SendApplyList() {
  const [applies, setApplies] = useState<ChatApply[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);
  const [filter, setFilter] = useState('all');
  const navigate = useNavigate();

  /** 服务端按状态分页拉取;append=true 追加(加载更多),否则替换 */
  const loadApplies = async (targetPage: number, statusKey: string, append = false) => {
    if (append) setLoadingMore(true);
    else setLoading(true);
    try {
      const status = statusKey === 'all' ? undefined : Number(statusKey);
      const res = await bridgeApi.sentApplies(targetPage, PAGE_SIZE, status);
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
  };

  useEffect(() => {
    loadApplies(1, filter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  const handleTabChange = (key: string) => {
    if (key !== filter) setFilter(key);
  };

  const handleRowClick = (apply: ChatApply) => {
    if (apply.status === 1 && apply.toUserId) {
      navigate(`/messages/chat?userId=${apply.toUserId}&name=${encodeURIComponent(apply.toUserNickname || '')}&avatar=${encodeURIComponent(apply.toUserAvatar || '')}`);
    }
  };

  /** 撤回待处理申请 */
  const handleWithdraw = async (apply: ChatApply) => {
    try {
      await bridgeApi.withdrawApply(apply.id);
      setApplies((prev) => prev.map((a) =>
        a.id === apply.id ? { ...a, status: 3, statusDesc: '已撤回' } : a));
      showToast('已撤回申请');
    } catch (e) {
      // 已被处理等具体原因透出服务端文案
      showToast((e as Error)?.message || '撤回失败');
    }
  };

  const formatTime = (dateStr: string): string => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - d.getTime();
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
    return d.toLocaleDateString('zh-CN');
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/bridge')}>←</button>
        <h2>我发出的申请</h2>
      </div>

      {/* 状态筛选 Tab(服务端筛选) */}
      <div className="subpage-tabs">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.key}
            className={`subpage-tab ${filter === tab.key ? 'active' : ''}`}
            onClick={() => handleTabChange(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 申请列表 */}
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}

        {!loading && applies.map((apply) => (
          <div
            key={apply.id}
            className="apply-row"
            onClick={() => handleRowClick(apply)}
          >
            <div className="apply-avatar">
              {apply.toUserAvatar ? (
                <img src={apply.toUserAvatar} alt="" />
              ) : (
                <AppIcon name="user" size={20} />
              )}
            </div>
            <div className="apply-info">
              <span className="apply-name">{apply.toUserNickname || '用户'}</span>
              <span className="apply-time">{formatTime(apply.applyTime)}</span>
            </div>
            <div className="apply-right">
              {apply.status === 0 && (
                <button className="apply-withdraw" onClick={(e) => { e.stopPropagation(); handleWithdraw(apply); }}>
                  撤回
                </button>
              )}
              <span className={`status-badge ${apply.status === 0 ? 'pending' : apply.status === 1 ? 'approved' : 'rejected'}`}>
                {apply.statusDesc}
              </span>
            </div>
          </div>
        ))}

        {!loading && applies.length === 0 && (
          <EmptyState icon="send" message="还没有发出过申请" />
        )}

        {/* 加载更多 */}
        {!loading && applies.length > 0 && (
          <div className="apply-loadmore">
            {hasMore ? (
              <button disabled={loadingMore} onClick={() => loadApplies(page + 1, filter, true)}>
                {loadingMore ? '加载中...' : '加载更多'}
              </button>
            ) : (
              <span className="apply-end">没有更多了</span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
