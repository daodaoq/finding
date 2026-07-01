import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { homeApi } from '../../api/home';
import BannerCarousel from '../../components/BannerCarousel';
import UserCard from '../../components/UserCard';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { useAuthStore } from '../../store/authStore';
import { QUICK_ACTIONS } from '../../utils/constants';
import type { HomeFeedUser, Banner } from '../../types/message';
import { showToast } from '../../components/Toast';
import './index.css';

export default function HomePage() {
  const [banners, setBanners] = useState<Banner[]>([]);
  const [feed, setFeed] = useState<HomeFeedUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();

  useEffect(() => {
    loadBanners();
    loadFeed(1);
  }, []);

  const loadBanners = async () => {
    try {
      const res = await homeApi.banners();
      setBanners(res.data.data);
    } catch { /* ignore */ }
  };

  const loadFeed = async (p: number) => {
    setLoading(true);
    try {
      const res = await homeApi.feed({ page: p, size: 10 });
      const data = res.data.data;
      if (p === 1) setFeed(data.records);
      else setFeed((prev) => [...prev, ...data.records]);
      setHasMore(data.hasMore);
      setPage(p);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const handleLike = async (userId: number) => {
    try {
      setFeed((prev) => prev.map((u) =>
        u.userId === userId ? { ...u, isLiked: !u.isLiked } : u));
    } catch { showToast('操作失败'); }
  };

  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const bottom = e.currentTarget.scrollHeight - e.currentTarget.scrollTop <=
                   e.currentTarget.clientHeight + 100;
    if (bottom && hasMore && !loading) loadFeed(page + 1);
  };

  return (
    <div className="home-page" onScroll={handleScroll}>
      {/* Header */}
      <div className="home-header">
        <div className="home-user" onClick={() => navigate('/mine')}>
          <div className="home-avatar">
            {user?.avatar ? <img src={user.avatar} alt="" /> : <span>👤</span>}
          </div>
          <span className="home-nickname">{user?.nickname || '点击登录'}</span>
        </div>
        <div className="home-actions">
          <button className="icon-btn" onClick={() => {}}>🔍</button>
          <button className="icon-btn" onClick={() => navigate('/messages')}>🔔</button>
        </div>
      </div>

      {/* Banner */}
      {banners.length > 0 && <BannerCarousel banners={banners} />}

      {/* Quick Actions */}
      <div className="quick-actions">
        <span className="section-label">心动匹配</span>
        <div className="quick-actions-row">
          {QUICK_ACTIONS.map((a) => (
            <button key={a.key} className="quick-action-item">
              <span className="quick-action-icon">{a.icon}</span>
              <span className="quick-action-label">{a.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Feed */}
      <div className="home-feed">
        <span className="section-label">动态信息流</span>
        {feed.map((u) => (
          <UserCard key={u.userId} user={u} onLike={handleLike} />
        ))}
        {loading && <LoadingSkeleton />}
        {!loading && feed.length === 0 && <EmptyState message="暂无推荐用户" />}
        {!hasMore && feed.length > 0 && (
          <p className="no-more">— 没有更多了 —</p>
        )}
      </div>
    </div>
  );
}
