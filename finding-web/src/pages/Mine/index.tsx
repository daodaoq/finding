import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { authApi } from '../../api/auth';
import { showToast } from '../../components/Toast';
import AppIcon, { type AppIconName } from '../../components/AppIcon';
import { APP_CONFIG } from '../../utils/config';
import type { User } from '../../types/user';
import './index.css';

const QUICK_ITEMS: { key: string; icon: AppIconName; label: string; to: string }[] = [
  { key: 'likes', icon: 'heart', label: '我的点赞', to: '/mine/likes' }, { key: 'joined', icon: 'calendar', label: '我的预约', to: '/mine/joined' },
  { key: 'posts', icon: 'message', label: '我的圈子', to: '/mine/posts' }, { key: 'history', icon: 'refresh', label: '历史记录', to: '/mine/history' },
];
const MENU_ITEMS: { key: string; icon: AppIconName; label: string; desc: string; to: string }[] = [
  { key: 'my-mates', icon: 'users', label: '我的搭子', desc: '关注和粉丝', to: '/mine/mates' }, { key: 'orders', icon: 'calendar', label: '订单', desc: '预约成功的搭子', to: '/mine/orders' },
  { key: 'help', icon: 'message', label: '帮助与客服', desc: '常见问题与联系客服', to: '/mine/help' }, { key: 'invitations', icon: 'inbox', label: '搭子管理', desc: '我发布的内容', to: '/mine/invitations' },
  { key: 'settings', icon: 'pen', label: '设置', desc: '个人资料与账号安全', to: '/mine/settings' }, { key: 'about', icon: 'book', label: '关于我们', desc: '版本更新信息', to: '/mine/about' },
];

export default function MinePage() {
  const user = useAuthStore((state) => state.user); const isLoggedIn = useAuthStore((state) => state.isLoggedIn); const setUser = useAuthStore((state) => state.setUser); const logout = useAuthStore((state) => state.logout);
  const navigate = useNavigate(); const [profile, setProfile] = useState<User | null>(user);
  useEffect(() => { if (isLoggedIn) authApi.getMe().then((res) => { setProfile(res.data.data); setUser(res.data.data); }).catch(() => showToast('加载个人信息失败')); }, [isLoggedIn, setUser]);
  const open = (to: string) => isLoggedIn ? navigate(to) : showToast('请先登录');
  if (!isLoggedIn) return <div className="mine-page"><div className="mine-top-guest"><div className="guest-avatar-lg"><AppIcon name="user" size={32} /></div><h2 className="guest-title">登录 Finding</h2><p className="guest-sub">{APP_CONFIG.SCHOOL_NAME}学生专属社交平台</p><button className="guest-login-btn" onClick={() => navigate('/login')}>手机号登录 / 注册</button></div></div>;
  const displayUser = profile || user; const initial = (displayUser?.nickname || '我').slice(0, 1);
  return <div className="mine-page"><header className="mine-header"><button className="mine-avatar" onClick={() => navigate('/mine/profile')} aria-label="编辑头像">{displayUser?.avatar ? <img src={displayUser.avatar} alt="" /> : <span>{initial}</span>}</button><div className="mine-header-info"><span className="mine-nickname">{displayUser?.nickname || '未设置昵称'}</span><button className="mine-school" onClick={() => navigate('/mine/verify')}>{displayUser?.school || APP_CONFIG.SCHOOL_NAME}{displayUser?.realNameVerified === 2 && <span className="verified-badge">已认证</span>}</button>{displayUser?.realNameVerified !== 2 && <button className="verify-hint" onClick={() => navigate('/mine/verify')}>{displayUser?.realNameVerified === 1 ? '认证审核中' : displayUser?.realNameVerified === 3 ? '认证未通过' : '完成学生认证'}</button>}</div><button className="mine-edit-btn" onClick={() => navigate('/mine/profile')}>编辑资料</button></header>
    <section className="mine-quick-grid">{QUICK_ITEMS.map((item) => <button key={item.key} className="mine-quick-item" onClick={() => open(item.to)}><AppIcon name={item.icon} size={20} /><span className="mine-quick-label">{item.label}</span></button>)}</section>
    <button className="mine-resume-banner" onClick={() => open('/mine/resume')}><AppIcon name="book" size={22} /><span className="mine-resume-info"><span className="mine-resume-title">情感简历</span><span className="mine-resume-sub">填写或查看我的情感简历</span></span><span className="mine-resume-action">查看</span></button>
    <section className="mine-menu">{MENU_ITEMS.map((item) => <button key={item.key} className="mine-menu-item" onClick={() => open(item.to)}><AppIcon name={item.icon} size={19} className="menu-item-icon" /><span className="menu-item-info"><span className="menu-item-label">{item.label}</span><span className="menu-item-desc">{item.desc}</span></span><span className="menu-item-action">查看</span></button>)}</section>
    <button className="mine-logout-btn" onClick={() => { logout(); navigate('/login'); }}>退出登录</button><div className="mine-version">Finding {APP_CONFIG.VERSION} · {APP_CONFIG.SCHOOL_NAME}</div>
  </div>;
}
