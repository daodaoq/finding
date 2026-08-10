import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mateApi } from '../../api/mate';
import SearchBar from '../../components/SearchBar';
import CategoryGrid from '../../components/CategoryGrid';
import MateCard from '../../components/MateCard';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import AppIcon from '../../components/AppIcon';
import { showToast } from '../../components/Toast';
import { useInfiniteList } from '../../hooks/useInfiniteList';
import { useGeolocation } from '../../hooks/useGeolocation';
import { MATE_CATEGORIES } from '../../utils/constants';
import type { Mate, MateCategory } from '../../types/mate';
import './index.css';

export default function MatePage() {
  const [category, setCategory] = useState('');
  const navigate = useNavigate();
  const { lat, lng } = useGeolocation(true);

  const { items: mates, loading, hasMore, setItems: setMates, onScroll } =
    useInfiniteList<Mate, [string, number?, number?]>({
      fetcher: async (p, cat, la, ln) => {
        const params: Record<string, unknown> = { page: p, size: 10 };
        if (cat) params.category = cat;
        if (la != null && ln != null) { params.lat = la; params.lng = ln; }
        const res = await mateApi.list(params);
        return res.data.data;
      },
      args: [category, lat, lng],
      deps: [category, lat, lng],
      onError: () => showToast('加载失败'),
    });

  const handleJoin = async (id: number) => {
    try {
      await mateApi.join(id);
      setMates((prev) => prev.map((m) =>
        m.id === id ? { ...m, hasJoined: true, currentParticipants: m.currentParticipants + 1 } : m));
    } catch { showToast('操作失败'); }
  };

  return (
    <div className="mate-page" onScroll={onScroll}>
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
        <button className="filter-search"><AppIcon name="search" size={18} /></button>
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