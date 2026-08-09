import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mateApi } from '../../api/mate';
import SearchBar from '../../components/SearchBar';
import MateCard from '../../components/MateCard';
import LoginModal from '../../components/LoginModal';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { useInfiniteList } from '../../hooks/useInfiniteList';
import { useGeolocation } from '../../hooks/useGeolocation';
import { showToast } from '../../components/Toast';
import { MATE_CATEGORIES } from '../../utils/constants';
import type { Mate } from '../../types/mate';
// 共享信息流样式（分类网格 + 排序栏 + 搭子列表 + .no-more）
import '../../components/Feed.css';
import './index.css';

/** 排序选项 */
const SORT_OPTIONS = [
  { key: 'time', label: '时间最近' },
  { key: 'distance', label: '距离最近' },
] as const;

export default function SquarePage() {
  const [category, setCategory] = useState('');
  const [sortBy, setSortBy] = useState('time');
  const navigate = useNavigate();
  const { showLogin, requireLogin, handleLoginSuccess, handleClose } = useRequireLogin();
  const { lat, lng } = useGeolocation(true);

  const { items: mates, loading, hasMore, setItems: setMates, onScroll } =
    useInfiniteList<Mate, [string, string, number?, number?]>({
      // 切换分类或排序时自动重置并加载第一页
      fetcher: async (p, cat, sort, la, ln) => {
        const params: Record<string, unknown> = { page: p, size: 10 };
        if (cat) params.category = cat;
        params.sortBy = sort === 'distance' ? 'distance' : 'time';
        if (la != null && ln != null) { params.lat = la; params.lng = ln; }
        const res = await mateApi.list(params);
        return res.data.data;
      },
      args: [category, sortBy, lat, lng],
      deps: [category, sortBy, lat, lng],
      onError: () => showToast('加载失败'),
    });

  const handleJoin = (id: number) => {
    requireLogin(async () => {
      try {
        await mateApi.join(id);
        setMates((prev) => prev.map((m) =>
          m.id === id ? { ...m, hasJoined: true, currentParticipants: m.currentParticipants + 1 } : m));
      } catch { showToast('操作失败'); }
    });
  };

  return (
    <div className="square-page" onScroll={onScroll}>
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