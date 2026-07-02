import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { authApi } from '../../api/auth';
import { showToast } from '../../components/Toast';
import { APP_CONFIG } from '../../utils/config';
import type { User } from '../../types/user';
import './index.css';

/** 功能菜单项 */
const MENU_ITEMS = [
  { key: 'my-posts', icon: '📝', label: '我的动态', desc: '我发布的所有动态' },
  { key: 'my-likes', icon: '❤️', label: '我的点赞', desc: '我点赞过的动态' },
  { key: 'my-mates', icon: '👫', label: '我的搭子', desc: '关注和粉丝' },
  { key: 'my-invitations', icon: '📋', label: '我发布的邀约', desc: '我创建的搭子活动' },
  { key: 'my-joined', icon: '📅', label: '我加入的搭子', desc: '已预约的搭子活动' },
] as const;

const BOTTOM_ITEMS = [
  { key: 'messages', icon: '💬', label: '消息中心', desc: '互动通知和私聊' },
  { key: 'about', icon: 'ℹ️', label: '关于我们', desc: 'Finding 团队' },
] as const;

export default function MinePage() {
  const user = useAuthStore((s) => s.user);
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const setUser = useAuthStore((s) => s.setUser);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();
  const [profile, setProfile] = useState<User | null>(user);

  useEffect(() => {
    if (isLoggedIn) {
      loadProfile();
    }
  }, [isLoggedIn]);

  const loadProfile = async () => {
    try {
      const res = await authApi.getMe();
      setProfile(res.data.data);
      setUser(res.data.data);
    } catch { showToast('加载个人信息失败'); }
  };

  const handleMenuClick = (key: string) => {
    if (!isLoggedIn) {
      showToast('请先登录');
      return;
    }
    switch (key) {
      case 'my-posts': navigate('/mine/posts'); break;
      case 'my-likes': navigate('/mine/likes'); break;
      case 'my-mates': navigate('/mine/mates'); break;
      case 'my-invitations': navigate('/mine/invitations'); break;
      case 'my-joined': navigate('/mine/joined'); break;
      case 'messages': navigate('/messages'); break;
      case 'about':
        showToast(`Finding ${APP_CONFIG.DESCRIPTION} ${APP_CONFIG.VERSION}\n${APP_CONFIG.SCHOOL_NAME}`);
        break;
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // ── 未登录状态 ──
  if (!isLoggedIn) {
    return (
      <div className="mine-page">
        <div className="mine-top-guest">
          <div className="guest-avatar-lg">👤</div>
          <h2 className="guest-title">登录 Finding</h2>
          <p className="guest-sub">{APP_CONFIG.SCHOOL_NAME}学生专属社交平台</p>
          <button className="guest-login-btn" onClick={() => navigate('/login')}>
            手机号登录 / 注册
          </button>
        </div>
      </div>
    );
  }

  // ── 已登录状态 ──
  const displayUser = profile || user;
  return (
    <div className="mine-page">
      {/* 用户信息头部 */}
      <div className="mine-header">
        <div className="mine-avatar" onClick={() => navigate('/mine/profile')}>
          {displayUser?.avatar
            ? <img src={displayUser.avatar} alt="" />
            : <span>👤</span>
          }
        </div>
        <div className="mine-header-info">
          <span className="mine-nickname">{displayUser?.nickname || '未设置昵称'}</span>
          <span className="mine-school">
            {displayUser?.school || APP_CONFIG.SCHOOL_NAME}
            {displayUser?.realNameVerified === 2 && <span className="verified-badge">✓ 已认证</span>}
          </span>
          {displayUser?.realNameVerified !== 2 && (
            <span className="verify-hint" onClick={() => navigate('/mine/verify')}>
              ⚠️ 点击完成学生认证
            </span>
          )}
        </div>
        <button className="mine-edit-btn" onClick={() => navigate('/mine/profile')}>
          编辑
        </button>
      </div>

      {/* 统计数据 — 点击可跳转 */}
      <div className="mine-stats">
        <div className="stat-item" onClick={() => navigate('/mine/posts')}>
          <span className="stat-num">{displayUser?.postCount || 0}</span>
          <span className="stat-label">动态</span>
        </div>
        <div className="stat-item" onClick={() => navigate('/mine/mates?tab=following')}>
          <span className="stat-num">{displayUser?.followingCount || 0}</span>
          <span className="stat-label">关注</span>
        </div>
        <div className="stat-item" onClick={() => navigate('/mine/mates?tab=followers')}>
          <span className="stat-num">{displayUser?.followerCount || 0}</span>
          <span className="stat-label">粉丝</span>
        </div>
      </div>

      {/* 功能菜单 */}
      <div className="mine-menu">
        {MENU_ITEMS.map((item) => (
          <div key={item.key} className="mine-menu-item" onClick={() => handleMenuClick(item.key)}>
            <span className="menu-item-icon">{item.icon}</span>
            <div className="menu-item-info">
              <span className="menu-item-label">{item.label}</span>
              <span className="menu-item-desc">{item.desc}</span>
            </div>
            <span className="menu-item-arrow">›</span>
          </div>
        ))}
      </div>

      {/* 底部菜单 */}
      <div className="mine-menu" style={{ marginTop: 8 }}>
        {BOTTOM_ITEMS.map((item) => (
          <div key={item.key} className="mine-menu-item" onClick={() => handleMenuClick(item.key)}>
            <span className="menu-item-icon">{item.icon}</span>
            <div className="menu-item-info">
              <span className="menu-item-label">{item.label}</span>
              <span className="menu-item-desc">{item.desc}</span>
            </div>
            <span className="menu-item-arrow">›</span>
          </div>
        ))}
      </div>

      {/* 退出登录 */}
      <button className="mine-logout-btn" onClick={handleLogout}>
        退出登录
      </button>

      <div className="mine-version">Finding {APP_CONFIG.VERSION} · {APP_CONFIG.SCHOOL_NAME}</div>
    </div>
  );
}
