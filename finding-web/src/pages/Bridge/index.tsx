import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bridgeApi, matchApi } from '../../api/bridge';
import { homeApi } from '../../api/home';
import BannerCarousel from '../../components/BannerCarousel';
import LoginModal from '../../components/LoginModal';
import EmptyState from '../../components/EmptyState';
import { showToast } from '../../components/Toast';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { useGeolocation } from '../../hooks/useGeolocation';
import PageState from '../../components/PageState';
import { getErrorMessage } from '../../utils/appError';
import { useAuthStore } from '../../store/authStore';
import { useBridgeStore } from '../../store/bridgeStore';
import { QUICK_ACTIONS } from '../../utils/constants';
import type { BridgeRecommendUser } from '../../types/bridge';
import type { Banner } from '../../types/message';
import AppIcon from '../../components/AppIcon';
import SwipeCard from './components/SwipeCard';
import './index.css';

export default function BridgePage() {
  const [banners, setBanners] = useState<Banner[]>([]);
  const navigate = useNavigate();
  const currentUser = useAuthStore((s) => s.user);
  const bridgePending = useBridgeStore((s) => s.pendingCount);
  const setBridgePending = useBridgeStore((s) => s.setPendingCount);
  const { showLogin, requireLogin, handleLoginSuccess, handleClose, openLogin, isLoggedIn } = useRequireLogin();
  const { lat, lng } = useGeolocation(true);

  const [candidate, setCandidate] = useState<BridgeRecommendUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [noMore, setNoMore] = useState(false);
  const [acting, setActing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // 竞态守卫:登录态/定位变化与手动刷新可能并发触发 loadNext,仅最新一次请求生效
  const loadSeqRef = useRef(0);

  useEffect(() => {
    loadBanners();
  }, []);

  // 拉取收到的待处理申请数，用于「来信」角标
  useEffect(() => {
    if (isLoggedIn) {
      bridgeApi.receivedPendingCount()
        .then((res) => setBridgePending(res.data.data?.count ?? 0))
        .catch(() => setBridgePending(0));
    }
  }, [isLoggedIn, setBridgePending]);

  // 登录态变化 / 定位就绪 → 重新加载推荐(定位就绪后带上坐标计算距离)
  useEffect(() => {
    if (isLoggedIn) {
      loadNext();
    } else {
      setCandidate(null);
      setNoMore(false);
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoggedIn, lat, lng]);

  const loadBanners = async () => {
    try {
      const res = await homeApi.banners();
      setBanners(res.data.data);
    } catch { /* 忽略 */ }
  };

  /** 拉取下一个推荐用户(每次一个;已跳过/已申请的用户会被后端排除) */
  const loadNext = async () => {
    const seq = ++loadSeqRef.current;
    setLoading(true);
    try {
      const params: { page: number; size: number; lat?: number; lng?: number } = { page: 1, size: 1 };
      if (lat != null && lng != null) { params.lat = lat; params.lng = lng; }
      const res = await bridgeApi.recommend(params);
      if (seq !== loadSeqRef.current) return; // 已被更新请求取代,丢弃过期响应
      const records = res.data.data.records || [];
      setNoMore(records.length === 0);
      setCandidate(records[0] || null);
      setError(null);
    } catch (e) {
      if (seq !== loadSeqRef.current) return;
      // 首次加载失败→错误态+重试;滑动中失败仅提示并保留当前卡
      if (!candidate) {
        setError(getErrorMessage(e, '加载推荐用户失败'));
      } else {
        showToast(getErrorMessage(e, '加载推荐用户失败'));
        setNoMore(true);
      }
    } finally {
      if (seq === loadSeqRef.current) {
        setLoading(false);
        setActing(false);
      }
    }
  };

  /** 心动(双向 match):互相心动即配对 */
  const handleLike = () => {
    if (!candidate || acting) return;
    requireLogin(() => {
      setActing(true);
      matchApi.like(candidate.userId)
        .then((res) => {
          showToast(res.data.data?.matched ? '匹配成功！去「互相喜欢」查看' : '已心动，等对方回应');
          loadNext();
        })
        .catch((e: any) => {
          showToast((e as Error)?.message || '操作失败');
          setActing(false);
        });
    });
  };

  /** 打招呼:发送聊天申请(单向申请→审批) */
  const handleApply = () => {
    if (!candidate || acting) return;
    requireLogin(() => {
      setActing(true);
      bridgeApi.apply(candidate.userId)
        .then(() => {
          showToast('申请已发送，等待对方回应');
          loadNext();
        })
        .catch((e: any) => {
          showToast((e as Error)?.message || '发送申请失败');
          setActing(false);
        });
    });
  };

  /** 不感兴趣:排除后换下一个 */
  const handleSkip = () => {
    if (!candidate || acting) return;
    requireLogin(() => {
      setActing(true);
      bridgeApi.skipUser(candidate.userId)
        .then(() => loadNext())
        .catch((e: any) => {
          showToast((e as Error)?.message || '操作失败');
          setActing(false);
        });
    });
  };

  const handleQuickAction = (key: string) => {
    switch (key) {
      case 'like':
        requireLogin(() => navigate('/bridge/send-apply'));
        break;
      case 'letter':
        requireLogin(() => navigate('/bridge/receive-apply'));
        break;
      case 'card':
        requireLogin(() => navigate('/bridge/my-card'));
        break;
      case 'matches':
        requireLogin(() => navigate('/bridge/matches'));
        break;
      case 'liked-me':
        requireLogin(() => navigate('/bridge/likes-received'));
        break;
    }
  };

  const handleRefresh = () => {
    if (isLoggedIn) loadNext();
  };

  const handleNotificationClick = () => {
    requireLogin(() => navigate('/messages/notifications'));
  };

  return (
    <div className="bridge-page">
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
              <span className={`bridge-quick-icon bridge-quick-icon--${action.key}`}>
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

      {/* 单卡推荐:一次一个用户,爱心喜欢/叉号换下一个 */}
      <div className="bridge-recommend-heading">
        <div>
          <h2>今日推荐</h2>
          <p>根据你的偏好，为你找到可能聊得来的人</p>
        </div>
        <span className="bridge-recommend-count">逐个认识</span>
      </div>

      <div className="bridge-swipe-section">
        {!isLoggedIn ? (
          <EmptyState
            icon="heart"
            message="登录后即可查看推荐用户"
            action={<button onClick={openLogin}>去登录</button>}
          />
        ) : loading ? (
          <div className="bridge-swipe-loading">加载中...</div>
        ) : error ? (
          <PageState loading={false} error={error} onRetry={() => loadNext()} />
        ) : noMore || !candidate ? (
          <EmptyState
            icon="heart"
            message="没有更多推荐了"
            action={<button onClick={handleRefresh}>刷新看看</button>}
          />
        ) : (
          <SwipeCard
            user={candidate}
            onLike={handleLike}
            onApply={handleApply}
            onSkip={handleSkip}
            disabled={acting}
          />
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
