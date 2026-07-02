import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bridgeApi } from '../../api/bridge';
import { homeApi } from '../../api/home';
import BannerCarousel from '../../components/BannerCarousel';
import UserCard from '../../components/UserCard';
import LoginModal from '../../components/LoginModal';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { useAuthStore } from '../../store/authStore';
import { QUICK_ACTIONS } from '../../utils/constants';
import type { BridgeRecommendUser } from '../../types/bridge';
import type { Banner } from '../../types/message';
import './index.css';

export default function BridgePage() {
  const [banners, setBanners] = useState<Banner[]>([]);
  const [users, setUsers] = useState<BridgeRecommendUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const navigate = useNavigate();
  const currentUser = useAuthStore((s) => s.user);
  const { showLogin, requireLogin, handleLoginSuccess, handleClose, isLoggedIn } = useRequireLogin();

  useEffect(() => {
    loadBanners();
    loadUsers(1);
  }, []);

  const loadBanners = async () => {
    try {
      const res = await homeApi.banners();
      setBanners(res.data.data);
    } catch { /* ignore */ }
  };

  const loadUsers = async (p: number) => {
    setLoading(true);
    try {
      const res = await bridgeApi.recommend({ page: p, size: 10 });
      const data = res.data.data;
      if (p === 1) setUsers(data.records);
      else setUsers((prev) => [...prev, ...data.records]);
      setHasMore(data.hasMore);
      setPage(p);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const handleLike = (userId: number) => {
    requireLogin(async () => {
      try {
        await bridgeApi.apply(userId);
        setUsers((prev) =>
          prev.map((u) => (u.userId === userId ? { ...u, isLiked: true } : u))
        );
      } catch { /* error handled by interceptor */ }
    });
  };

  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const bottom =
      e.currentTarget.scrollHeight - e.currentTarget.scrollTop <=
      e.currentTarget.clientHeight + 100;
    if (bottom && hasMore && !loading) {
      loadUsers(page + 1);
    }
  };

  const handleQuickAction = (key: string) => {
    switch (key) {
      case 'like':
        requireLogin(() => navigate('/bridge/send-apply'));
        break;
      case 'letter':
        requireLogin(() => navigate('/bridge/receive-apply'));
        break;
      case 'watch':
      case 'game':
        // 功能开发中
        break;
    }
  };

  const handleNotificationClick = () => {
    requireLogin(() => navigate('/messages/notifications'));
  };

  const handleRefresh = () => {
    setUsers([]);
    setPage(1);
    setHasMore(true);
    loadUsers(1);
  };

  return (
    <div className="bridge-page" onScroll={handleScroll}>
      {/* 顶部导航栏 */}
      <div className="bridge-top-nav">
        <div className="bridge-nav-left">
          <div className="bridge-nav-avatar">
            {currentUser?.avatar ? (
              <img src={currentUser.avatar} alt="" />
            ) : (
              <span>👤</span>
            )}
          </div>
          <span className="bridge-nav-nickname">
            {currentUser?.nickname || '游客'}
          </span>
        </div>
        <div className="bridge-nav-right">
          <button className="bridge-nav-icon-btn" onClick={handleRefresh}>
            🔄
          </button>
          <button className="bridge-nav-icon-btn">🔍</button>
          <button className="bridge-nav-icon-btn" onClick={handleNotificationClick}>
            🔔
          </button>
        </div>
      </div>

      {/* Banner 轮播 */}
      {banners.length > 0 && (
        <div className="bridge-banner-wrap">
          <BannerCarousel banners={banners} />
        </div>
      )}

      {/* 心动匹配快捷入口 */}
      <div className="bridge-quick-section">
        <div className="bridge-quick-title">心动匹配</div>
        <div className="bridge-quick-icons">
          {QUICK_ACTIONS.map((action) => (
            <button
              key={action.key}
              className="bridge-quick-item"
              onClick={() => handleQuickAction(action.key)}
            >
              <span className="bridge-quick-icon">{action.icon}</span>
              <span className="bridge-quick-label">{action.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 推荐用户信息流 */}
      <div className="bridge-feed-header">推荐用户</div>
      <div className="bridge-user-list">
        {users.map((user) => (
          <UserCard key={user.userId} user={user} onLike={handleLike} />
        ))}
        {loading && <LoadingSkeleton />}
        {!loading && users.length === 0 && (
          <EmptyState icon="💕" message="暂无推荐用户，换个时间再来看看吧" />
        )}
        {!hasMore && users.length > 0 && (
          <p className="no-more">— 没有更多了 —</p>
        )}
      </div>

      {/* 登录弹窗 */}
      <LoginModal
        visible={showLogin}
        onClose={handleClose}
        onSuccess={handleLoginSuccess}
      />
    </div>
  );
}
