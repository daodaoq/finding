import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { matchApi } from '../../api/bridge';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { showToast } from '../../components/Toast';
import AppIcon from '../../components/AppIcon';
import { formatRelativeTime } from '../../utils/format';
import type { MatchUser } from '../../types/bridge';
import './subpage.css';

export type MatchListMode = 'sent' | 'received' | 'matches';

const CONFIG: Record<MatchListMode, { title: string; empty: string; emptyIcon: 'heart' | 'send' | 'sparkles' }> = {
  sent: { title: '我喜欢的', empty: '还没有心动的人', emptyIcon: 'heart' },
  received: { title: '谁喜欢我', empty: '还没有人喜欢你', emptyIcon: 'send' },
  matches: { title: '互相喜欢', empty: '还没有互相喜欢的人', emptyIcon: 'sparkles' },
};

export default function MatchListPage({ mode }: { mode: MatchListMode }) {
  const navigate = useNavigate();
  const [list, setList] = useState<MatchUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const cfg = CONFIG[mode];

  const fetcher = mode === 'sent' ? matchApi.likesSent : mode === 'received' ? matchApi.likesReceived : matchApi.matches;

  const load = async (p: number, append: boolean) => {
    append ? setLoadingMore(true) : setLoading(true);
    try {
      const res = await fetcher(p, 20);
      const data = res.data.data;
      setList((prev) => (append ? [...prev, ...data.records] : data.records));
      setHasMore(data.hasMore ?? false);
      setPage(p);
    } catch {
      showToast('加载失败');
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  useEffect(() => { load(1, false); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [mode]);

  /** 回喜欢(仅「谁喜欢我」且未配对时) */
  const likeBack = async (id: number) => {
    try {
      await matchApi.like(id);
      setList((prev) => prev.map((m) => (m.userId === id ? { ...m, isMatched: true } : m)));
      showToast('已心动');
    } catch {
      showToast('操作失败');
    }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/bridge')}>←</button>
        <h2>{cfg.title}</h2>
      </div>

      <div className="subpage-list">
        {loading && <LoadingSkeleton />}

        {!loading && list.map((m) => (
          <div key={m.userId} className="apply-row" onClick={() => navigate(`/user/${m.userId}`)}>
            <div className="apply-avatar">
              {m.avatar ? <img src={m.avatar} alt="" /> : <AppIcon name="user" size={20} />}
            </div>
            <div className="apply-info">
              <span className="apply-name">
                {m.nickname || '用户'}
                {m.verified === 1 && <span className="mine-verified" style={{ marginLeft: 6 }}>已认证</span>}
              </span>
              <span className="apply-time">
                {m.school || m.signature || '这个人很神秘'}
                {' · '}
                {formatRelativeTime(m.time)}
              </span>
            </div>
            <div className="apply-right">
              {mode === 'received' && !m.isMatched && (
                <button
                  className="apply-withdraw"
                  onClick={(e) => { e.stopPropagation(); likeBack(m.userId); }}
                >
                  回喜欢
                </button>
              )}
              <span className={`status-badge ${m.isMatched ? 'approved' : 'pending'}`}>
                {m.isMatched ? '互相喜欢' : mode === 'received' ? '喜欢我' : '已心动'}
              </span>
            </div>
          </div>
        ))}

        {!loading && list.length === 0 && <EmptyState icon={cfg.emptyIcon} message={cfg.empty} />}

        {!loading && list.length > 0 && (
          <div className="apply-loadmore">
            {hasMore ? (
              <button disabled={loadingMore} onClick={() => load(page + 1, true)}>
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
