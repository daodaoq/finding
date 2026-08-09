import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { authApi } from '../../api/auth';
import { showToast } from '../../components/Toast';
import { APP_CONFIG } from '../../utils/config';
import type { User } from '../../types/user';
import './index.css';

/** 名片下方的四个快捷按钮 */
const QUICK_ITEMS = [
  { key: 'likes', icon: '❤️', label: '我的点赞', to: '/mine/likes' },
  { key: 'joined', icon: '📅', label: '我的预约', to: '/mine/joined' },
  { key: 'posts', icon: '🌐', label: '我的圈子', to: '/mine/posts' },
  { key: 'history', icon: '🕐', label: '历史记录', to: '/mine/history' },
] as const;

/** 主菜单 */
const MENU_ITEMS = [
  { key: 'my-mates', icon: '👫', label: '我的搭子', desc: '关注和粉丝', to: '/mine/mates' },
  { key: 'orders', icon: '🧾', label: '订单', desc: '预约成功的搭子', to: '/mine/orders' },
  { key: 'help', icon: '🎧', label: '帮助与客服', desc: '常见问题 · 联系客服', to: '/mine/help' },
  { key: 'invitations', icon: '📋', label: '搭子管理', desc: '我发布的内容', to: '/mine/invitations' },
  { key: 'settings', icon: '⚙️', label: '设置', desc: '个人资料 · 账号与安全', to: '/mine/settings' },
  { key: 'about', icon: 'ℹ️', label: '关于我们', desc: '版本更新信息', to: '/mine/about' },
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

  const handleMenuClick = (key: string, to?: string) => {
    if (!isLoggedIn) {
      showToast('请先登录');
      return;
    }
    if (to) navigate(to);
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
      {/* 名片 */}
      <div className="mine-header">
        <div className="mine-avatar" onClick={() => navigate('/mine/profile')}>
          {displayUser?.avatar
            ? <img src={displayUser.avatar} alt="" />
            : <span>👤</span>
          }
        </div>
        <div className="mine-header-info">
          <span className="mine-nickname">{displayUser?.nickname || '未设置昵称'}</span>
          <span
            className="mine-school"
            onClick={() => navigate('/mine/verify')}
            style={{ cursor: 'pointer' }}
            title="点击查看我的认证信息"
          >
            {displayUser?.school || APP_CONFIG.SCHOOL_NAME}
            {displayUser?.realNameVerified === 2 && (
              <span className="verified-badge">✓ 已认证</span>
            )}
          </span>
          {displayUser?.realNameVerified !== 2 && (
            <span className="verify-hint" onClick={() => navigate('/mine/verify')}>
              {displayUser?.realNameVerified === 1
                ? '⏳ 认证审核中，点击查看'
                : displayUser?.realNameVerified === 3
                  ? '❌ 认证未通过，点击查看'
                  : '⚠️ 点击完成学生认证'}
            </span>
          )}
        </div>
        <button className="mine-edit-btn" onClick={() => navigate('/mine/profile')}>
          编辑
        </button>
      </div>

      {/* 四按钮 */}
      <div className="mine-quick-grid">
        {QUICK_ITEMS.map((item) => (
          <div key={item.key} className="mine-quick-item" onClick={() => handleMenuClick(item.key, item.to)}>
            <span className="mine-quick-icon">{item.icon}</span>
            <span className="mine-quick-label">{item.label}</span>
          </div>
        ))}
      </div>

      {/* 情感简历横屏卡 */}
      <div className="mine-resume-banner" onClick={() => handleMenuClick('resume', '/mine/resume')}>
        <span className="mine-resume-icon">💘</span>
        <div className="mine-resume-info">
          <span className="mine-resume-title">情感简历</span>
          <span className="mine-resume-sub">填写或查看我的情感简历</span>
        </div>
        <span className="mine-resume-arrow">›</span>
      </div>

      {/* 主菜单 */}
      <div className="mine-menu" style={{ marginTop: 8 }}>
        {MENU_ITEMS.map((item) => (
          <div key={item.key} className="mine-menu-item" onClick={() => handleMenuClick(item.key, item.to)}>
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
