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
  const [anonymousOnly, setAnonymousOnly] = useState(false);
  const [city, setCity] = useState('');
  const [daysAhead, setDaysAhead] = useState(0);
  const [availableOnly, setAvailableOnly] = useState(false);
  const navigate = useNavigate();
  const { lat, lng } = useGeolocation(true);

  const { items: mates, loading, hasMore, setItems: setMates, onScroll } =
    useInfiniteList<Mate, [string, number?, number?]>({
      fetcher: async (p, cat, la, ln) => {
        const params: Record<string, unknown> = { page: p, size: 10 };
        if (cat) params.category = cat;
        if (anonymousOnly) params.anonymousOnly = true;
        if (city.trim()) params.city = city.trim();
        if (daysAhead) params.daysAhead = daysAhead;
        if (availableOnly) params.availableOnly = true;
        if (la != null && ln != null) { params.latitude = la; params.longitude = ln; params.radiusKm = 20; }
        const res = await mateApi.list(params);
        return res.data.data;
      },
      args: [category, lat, lng],
      deps: [category, lat, lng, anonymousOnly, city, daysAhead, availableOnly],
      onError: () => showToast('加载失败'),
    });

  const handleJoin = async (id: number) => {
    const message = window.prompt('可以给发起人留一句话（选填，最多500字）', '') ?? undefined;
    if (message !== undefined && message.length > 500) { showToast('留言不能超过500字'); return; }
    try {
      await mateApi.join(id, message);
      // 服务端决定待审核/候补/已通过状态，重新拉取避免客户端伪造人数
      setMates((prev) => prev.filter((m) => m.id !== id));
    } catch (e: any) { showToast(e?.message || '操作失败'); }
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
          <input type="checkbox" checked={anonymousOnly} onChange={e => setAnonymousOnly(e.target.checked)} /> 匿名匹配
        </label>
          <label className="filter-item"><span>时间</span><select value={daysAhead} onChange={e => setDaysAhead(Number(e.target.value))}><option value={0}>不限时间</option><option value={1}>今天起</option><option value={7}>7天内</option><option value={30}>30天内</option></select></label>
          <label className="filter-item"><span>地点</span><input value={city} onChange={e => setCity(e.target.value)} placeholder="校区/区域" /></label>
          <label className="filter-anonymous"><input type="checkbox" checked={availableOnly} onChange={e => setAvailableOnly(e.target.checked)} /> 有空位</label>
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
