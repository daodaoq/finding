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
  { key: 'likes', icon: 'heart', label: '我的点赞', to: '/mine/likes' },
  { key: 'joined', icon: 'calendar', label: '我的预约', to: '/mine/joined' },
  { key: 'posts', icon: 'message', label: '我的圈子', to: '/mine/posts' },
  { key: 'history', icon: 'refresh', label: '历史记录', to: '/mine/history' },
];
const MENU_ITEMS: { key: string; icon: AppIconName; label: string; desc: string; to: string }[] = [
  { key: 'orders', icon: 'calendar', label: '订单', desc: '预约成功的搭子', to: '/mine/orders' },
  { key: 'reports', icon: 'flag', label: '我的投诉', desc: '查看举报处理结果', to: '/mine/reports' },
  { key: 'help', icon: 'message', label: '帮助与客服', desc: '常见问题与联系渠道', to: '/mine/help' },
  { key: 'applications', icon: 'inbox', label: '搭子申请', desc: '待审核、候补与历史记录', to: '/mine/applications' },
  { key: 'invitations', icon: 'calendar', label: '搭子管理', desc: '我发布的内容', to: '/mine/invitations' },
  { key: 'settings', icon: 'settings', label: '设置', desc: '个人资料与账号安全', to: '/mine/settings' },
  { key: 'about', icon: 'book', label: '关于我们', desc: '版本更新信息', to: '/mine/about' },
];

export default function MinePage() {
  const user = useAuthStore((state) => state.user);
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const setUser = useAuthStore((state) => state.setUser);
  const logout = useAuthStore((state) => state.logout);
  const navigate = useNavigate();
  const [profile, setProfile] = useState<User | null>(user);

  useEffect(() => {
    if (!isLoggedIn) return;
    authApi.getMe().then((res) => { setProfile(res.data.data); setUser(res.data.data); }).catch(() => showToast('加载个人信息失败'));
  }, [isLoggedIn, setUser]);

  const open = (to: string) => isLoggedIn ? navigate(to) : showToast('请先登录');
  if (!isLoggedIn) return <div className="mine-page mine-page--guest"><section className="mine-guest-card"><div className="guest-avatar-lg"><AppIcon name="user" size={32} /></div><h1>登录 Finding</h1><p>{APP_CONFIG.SCHOOL_NAME}学生专属社交平台</p><button onClick={() => navigate('/login')}>手机号登录 / 注册</button></section></div>;

  const displayUser = profile || user;
  const initial = (displayUser?.nickname || '我').slice(0, 1);
  const verified = displayUser?.realNameVerified === 2;
  const cardStyle = displayUser?.profileBackground ? { backgroundImage: `url("${displayUser.profileBackground}")` } : undefined;
  return <main className="mine-page">
    <section className={`mine-profile-card ${displayUser?.profileBackground ? 'has-cover' : ''}`} style={cardStyle}>
      <div className="mine-profile-shade" />
      <button className="mine-cover-edit" onClick={() => navigate('/mine/profile')} aria-label="编辑个人资料卡背景"><AppIcon name="camera" size={15} /></button>
      <div className="mine-profile-main"><button className="mine-avatar" onClick={() => navigate('/mine/profile')} aria-label="编辑头像">{displayUser?.avatar ? <img src={displayUser.avatar} alt="" /> : <span>{initial}</span>}</button><div className="mine-profile-info"><div className="mine-name-row"><h1>{displayUser?.nickname || '未设置昵称'}</h1>{verified ? <span className="mine-verified">已认证</span> : null}</div><button className="mine-school" onClick={() => navigate(verified ? '/mine/profile' : '/mine/verify')}>{displayUser?.school || APP_CONFIG.SCHOOL_NAME}</button><p>{displayUser?.signature || '写一句签名，让大家更了解你'}</p></div><button className="mine-edit-btn" onClick={() => navigate('/mine/profile')}>编辑资料</button></div>
      <div className="mine-stat-row"><button onClick={() => open('/mine/mates?tab=following')}><b>{displayUser?.followingCount ?? 0}</b><span>关注</span></button><button onClick={() => open('/mine/mates?tab=followers')}><b>{displayUser?.followerCount ?? 0}</b><span>粉丝</span></button><button onClick={() => open('/mine/mates?tab=mutual')}><b>{displayUser?.mutualCount ?? 0}</b><span>好友</span></button></div>
    </section>
    <section className="mine-quick-card">{QUICK_ITEMS.map((item) => <button key={item.key} onClick={() => open(item.to)}><span><AppIcon name={item.icon} size={21} /></span><small>{item.label}</small></button>)}</section>
    <button className="mine-resume-card" onClick={() => open('/mine/resume')}><span className="mine-resume-icon"><AppIcon name="book" size={22} /></span><span className="mine-resume-copy"><b>情感简历</b><small>填写或查看我的情感简历</small></span><span className="mine-resume-action">查看 <AppIcon name="right" size={17} /></span></button>
    <section className="mine-menu-card">{MENU_ITEMS.map((item) => <button key={item.key} className="mine-menu-item" onClick={() => open(item.to)}><span className="mine-menu-icon"><AppIcon name={item.icon} size={21} /></span><span className="mine-menu-copy"><b>{item.label}</b><small>{item.desc}</small></span><AppIcon name="right" size={19} className="mine-menu-arrow" /></button>)}</section>
    <button className="mine-logout-btn" onClick={() => { logout(); navigate('/login'); }}>退出登录</button><p className="mine-version">Finding {APP_CONFIG.VERSION} · {APP_CONFIG.SCHOOL_NAME}</p>
  </main>;
}
