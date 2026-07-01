import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mateApi } from '../../api/mate';
import SearchBar from '../../components/SearchBar';
import MateCard from '../../components/MateCard';
import LoginModal from '../../components/LoginModal';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { MATE_CATEGORIES } from '../../utils/constants';
import type { Mate } from '../../types/mate';
import './index.css';

/** 排序选项 */
const SORT_OPTIONS = [
  { key: 'time', label: '时间最近' },
  { key: 'distance', label: '距离最近' },
] as const;

export default function SquarePage() {
  const [category, setCategory] = useState('');
  const [sortBy, setSortBy] = useState('time');
  const [mates, setMates] = useState<Mate[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const navigate = useNavigate();
  const { showLogin, requireLogin, handleLoginSuccess, handleClose } = useRequireLogin();

  // 切换分类或排序时重置
  useEffect(() => {
    setMates([]);
    setPage(1);
    setHasMore(true);
    loadMates(1, category, sortBy);
  }, [category, sortBy]);

  const loadMates = async (p: number, cat: string, sort: string) => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page: p, size: 10 };
      if (cat) params.category = cat;
      if (sort === 'time') params.sortBy = 'time';
      else params.sortBy = 'distance';

      const res = await mateApi.list(params);
      const data = res.data.data;
      if (p === 1) setMates(data.records);
      else setMates((prev) => [...prev, ...data.records]);
      setHasMore(data.hasMore);
      setPage(p);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const handleJoin = (id: number) => {
    requireLogin(async () => {
      try {
        await mateApi.join(id);
        setMates((prev) => prev.map((m) =>
          m.id === id ? { ...m, hasJoined: true, currentParticipants: m.currentParticipants + 1 } : m));
      } catch { /* ignore */ }
    });
  };

  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const bottom = e.currentTarget.scrollHeight - e.currentTarget.scrollTop <=
                   e.currentTarget.clientHeight + 100;
    if (bottom && hasMore && !loading) loadMates(page + 1, category, sortBy);
  };

  return (
    <div className="square-page" onScroll={handleScroll}>
      {/* 顶部搜索 */}
      <SearchBar placeholder="搜搭子..." />

      {/* 分类筛选网格 */}
      <div className="square-categories">
        <div className="category-grid">
          {MATE_CATEGORIES.map((cat) => (
            <button
              key={cat.code}
              className={`category-cell ${category === cat.code ? 'active' : ''}`}
              onClick={() => setCategory(category === cat.code ? '' : cat.code)}
            >
              <span className="category-cell-icon">{cat.icon}</span>
              <span className="category-cell-name">{cat.name}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 排序选项 */}
      <div className="square-sort-bar">
        <span className="sort-label">
          {category ? MATE_CATEGORIES.find(c => c.code === category)?.name : '全部搭子'}
        </span>
        <div className="sort-options">
          {SORT_OPTIONS.map((opt) => (
            <button
              key={opt.key}
              className={`sort-chip ${sortBy === opt.key ? 'active' : ''}`}
              onClick={() => setSortBy(opt.key)}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* 搭子列表 */}
      <div className="square-mate-list">
        {mates.map((mate) => (
          <MateCard
            key={mate.id}
            mate={mate}
            onJoin={handleJoin}
            onClick={(id) => navigate(`/mate/${id}`)}
          />
        ))}
        {loading && <LoadingSkeleton />}
        {!loading && mates.length === 0 && (
          <EmptyState icon="🔍" message={category ? `暂无"${MATE_CATEGORIES.find(c => c.code === category)?.name}"邀约` : '暂无搭子邀约'} />
        )}
        {!hasMore && mates.length > 0 && <p className="no-more">— 没有更多了 —</p>}
      </div>

      {/* 登录弹窗 */}
      <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />
    </div>
  );
}
