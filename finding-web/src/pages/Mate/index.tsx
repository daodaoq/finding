import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mateApi } from '../../api/mate';
import SearchBar from '../../components/SearchBar';
import CategoryGrid from '../../components/CategoryGrid';
import MateCard from '../../components/MateCard';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { MATE_CATEGORIES } from '../../utils/constants';
import type { Mate, MateCategory } from '../../types/mate';
import './index.css';

export default function MatePage() {
  const [category, setCategory] = useState('');
  const [mates, setMates] = useState<Mate[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    setMates([]);
    setPage(1);
    setHasMore(true);
    loadMates(1, category);
  }, [category]);

  const loadMates = async (p: number, cat: string) => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page: p, size: 10 };
      if (cat) params.category = cat;
      const res = await mateApi.list(params);
      const data = res.data.data;
      if (p === 1) setMates(data.records);
      else setMates((prev) => [...prev, ...data.records]);
      setHasMore(data.hasMore);
      setPage(p);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const handleJoin = async (id: number) => {
    try {
      await mateApi.join(id);
      setMates((prev) => prev.map((m) =>
        m.id === id ? { ...m, hasJoined: true, currentParticipants: m.currentParticipants + 1 } : m));
    } catch { /* ignore */ }
  };

  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const bottom = e.currentTarget.scrollHeight - e.currentTarget.scrollTop <=
                   e.currentTarget.clientHeight + 100;
    if (bottom && hasMore && !loading) loadMates(page + 1, category);
  };

  return (
    <div className="mate-page" onScroll={handleScroll}>
      {/* Header */}
      <div className="mate-header">
        <h2 className="mate-title">找搭子 → 资源圈</h2>
      </div>

      {/* Filter bar */}
      <div className="mate-filter">
        <label className="filter-anonymous">
          <input type="checkbox" /> 匿名匹配
        </label>
        <span className="filter-item">类型</span>
        <span className="filter-item">时间</span>
        <span className="filter-item">地点</span>
        <button className="filter-search">🔍</button>
      </div>

      {/* Categories */}
      <CategoryGrid
        categories={MATE_CATEGORIES as unknown as MateCategory[]}
        onSelect={(code) => setCategory(code === category ? '' : code)}
      />

      {/* Recommendation list */}
      <div className="mate-list-section">
        <div className="mate-list-header">
          <span>动态推荐</span>
          <span className="sort-hint">按距离/时间排序</span>
        </div>
        {mates.map((mate) => (
          <MateCard
            key={mate.id}
            mate={mate}
            onJoin={handleJoin}
            onClick={() => navigate(`/mate/${mate.id}`)}
          />
        ))}
        {loading && <LoadingSkeleton />}
        {!loading && mates.length === 0 && <EmptyState message="暂无搭子邀约" />}
        {!hasMore && mates.length > 0 && (
          <p className="no-more">— 没有更多了 —</p>
        )}
      </div>
    </div>
  );
}
