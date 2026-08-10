import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bridgeApi } from '../../api/bridge';
import { homeApi } from '../../api/home';
import BannerCarousel from '../../components/BannerCarousel';
import UserCard from '../../components/UserCard';
import LoginModal from '../../components/LoginModal';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { showToast } from '../../components/Toast';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { useInfiniteList } from '../../hooks/useInfiniteList';
import { useGeolocation } from '../../hooks/useGeolocation';
import { useAuthStore } from '../../store/authStore';
import { useBridgeStore } from '../../store/bridgeStore';
import { QUICK_ACTIONS } from '../../utils/constants';
import type { BridgeRecommendUser } from '../../types/bridge';
import type { Banner } from '../../types/message';
import AppIcon from '../../components/AppIcon';
import './index.css';

export default function BridgePage() {
  const [banners, setBanners] = useState<Banner[]>([]);
  const [applyTarget, setApplyTarget] = useState<number | null>(null);
  const [applyRemark, setApplyRemark] = useState('');
  const navigate = useNavigate();
  const currentUser = useAuthStore((s) => s.user);
  const bridgePending = useBridgeStore((s) => s.pendingCount);
  const setBridgePending = useBridgeStore((s) => s.setPendingCount);
  const { showLogin, requireLogin, handleLoginSuccess, handleClose, isLoggedIn } = useRequireLogin();

  const { lat, lng } = useGeolocation(true);

  const { items: users, loading, hasMore, setItems: setUsers, reset, onScroll } =
    useInfiniteList<BridgeRecommendUser, [number?, number?]>({
      fetcher: async (p, la, ln) => {
        const params: { page: number; size: number; lat?: number; lng?: number } = { page: p, size: 10 };
        if (la != null && ln != null) { params.lat = la; params.lng = ln; }
        const res = await bridgeApi.recommend(params);
        return res.data.data;
      },
      args: [lat, lng],
      onError: () => showToast('加载推荐用户失败'),
    });

  useEffect(() => {
    loadBanners();
  }, []);

  // 拉取收到的待处理申请数，用于「情书」「底部鹊桥」角标
  useEffect(() => {
    if (isLoggedIn) {
      bridgeApi.receivedPendingCount()
        .then((res) => setBridgePending(res.data.data?.count ?? 0))
        .catch(() => setBridgePending(0));
    }
  }, [isLoggedIn, setBridgePending]);

  const loadBanners = async () => {
    try {
      const res = await homeApi.banners();
      setBanners(res.data.data);
    } catch { showToast('加载Banner失败'); }
  };

  const handleLike = (userId: number) => {
    requireLogin(() => {
      setApplyTarget(userId);
      setApplyRemark('');
    });
  };

  const confirmApply = async () => {
    if (applyTarget == null) return;
    try {
      await bridgeApi.apply(applyTarget, applyRemark.trim() || undefined);
      setUsers((prev) =>
        prev.map((u) => (u.userId === applyTarget ? { ...u, isLiked: true } : u))
      );
      showToast('申请已发送');
    } catch { showToast('发送申请失败'); }
    finally { setApplyTarget(null); }
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
    reset();
  };

  return (
    <div className="bridge-page" onScroll={onScroll}>
      {/* 顶部导航栏 */}
      <div className="bridge-top-nav">
        <div className="bridge-nav-left">
          <div className="bridge-nav-avatar">
            {currentUser?.avatar ? (
              <img src={currentUser.avatar} alt="" />
            ) : (
              <AppIcon name="user" size={18} />
            )}
          </div>
          <span className="bridge-nav-nickname">
            {currentUser?.nickname || '游客'}
          </span>
        </div>
        <div className="bridge-nav-right">
          <button className="bridge-nav-icon-btn" onClick={handleRefresh}>
            <AppIcon name="refresh" size={19} />
          </button>
          <button className="bridge-nav-icon-btn" aria-label="搜索"><AppIcon name="search" size={19} /></button>
          <button className="bridge-nav-icon-btn" onClick={handleNotificationClick}>
            <AppIcon name="bell" size={19} />
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
              <span className="bridge-quick-icon">
                <AppIcon name={action.icon} size={22} />
                {action.key === 'letter' && bridgePending > 0 && (
                  <span className="bridge-quick-badge">
                    {bridgePending > 99 ? '99+' : bridgePending}
                  </span>
                )}
              </span>
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
          <EmptyState icon="heart" message="暂无推荐用户，换个时间再来看看吧" />
        )}
        {!hasMore && users.length > 0 && (
          <p className="no-more">— 没有更多了 —</p>
        )}
      </div>

      {/* 发送申请备注弹层 */}
      {applyTarget != null && (
        <div
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', zIndex: 1000, display: 'flex', alignItems: 'flex-end', justifyContent: 'center' }}
          onClick={() => setApplyTarget(null)}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{ width: '100%', maxWidth: 480, background: '#fff', borderRadius: '16px 16px 0 0', padding: 16 }}
          >
            <h4 style={{ margin: '0 0 12px' }}>发送心动申请</h4>
            <input
              placeholder="写一句介绍/想说的话（选填）"
              value={applyRemark}
              onChange={(e) => setApplyRemark(e.target.value)}
              maxLength={100}
              style={{ width: '100%', border: '1px solid #eee', borderRadius: 8, padding: 10, fontSize: 14, boxSizing: 'border-box' }}
            />
            <button
              onClick={confirmApply}
              style={{ width: '100%', marginTop: 16, padding: 12, border: 'none', borderRadius: 22, background: '#ff6b81', color: '#fff', fontSize: 15 }}
            >
              发送申请
            </button>
          </div>
        </div>
      )}

      {/* 登录弹窗 */}
      <LoginModal
        visible={showLogin}
        onClose={handleClose}
        onSuccess={handleLoginSuccess}
      />
    </div>
  );
}
