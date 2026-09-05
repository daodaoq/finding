import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AppIcon from './AppIcon';
import { BOTTOM_NAV_ITEMS } from '../utils/constants';
import { useMessageStore } from '../store/messageStore';
import { useBridgeStore } from '../store/bridgeStore';
import './TopNav.css';

interface Props { onCenterClick?: () => void; }

/** 桌面端顶部导航:条目/角标/激活逻辑与 BottomNav 完全一致,仅形态不同(移动端由 CSS 隐藏)。 */
export default function TopNav({ onCenterClick }: Props) {
  const location = useLocation();
  const navigate = useNavigate();
  const unreadCount = useMessageStore((s) => s.unreadCount);
  const bridgePending = useBridgeStore((s) => s.pendingCount);
  const [q, setQ] = useState('');
  const isActive = (item: typeof BOTTOM_NAV_ITEMS[number]) => !item.isCenter && (item.key === 'home' ? location.pathname === '/' : location.pathname.startsWith(item.path));

  const submitSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const t = q.trim();
    if (!t) return;
    setQ('');
    navigate(`/search?q=${encodeURIComponent(t)}`);
  };

  return <nav className="top-nav" aria-label="主导航">
    <div className="top-nav-inner">
      <span className="top-nav-brand">Finding</span>
      <form className="top-nav-search" onSubmit={submitSearch} role="search">
        <div className="top-nav-search-box">
          <AppIcon name="search" size={15} />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="搜索用户、动态、搭子..." aria-label="站内搜索" />
        </div>
      </form>
      {BOTTOM_NAV_ITEMS.map((item) => {
        const count = item.key === 'messages' ? unreadCount : item.key === 'bridge' ? bridgePending : 0;
        return <button key={item.key} className={`nav-item ${item.isCenter ? 'top-nav-item--center' : ''} ${isActive(item) ? 'top-nav-item--active' : ''}`} onClick={() => item.isCenter ? onCenterClick?.() : navigate(item.path)}>
          <span className="nav-icon"><AppIcon name={item.icon} size={17} /></span>
          <span className="nav-label">{item.label}{count > 0 && <sup className="nav-badge">{count > 99 ? '99+' : count}</sup>}</span>
        </button>;
      })}
    </div>
  </nav>;
}
