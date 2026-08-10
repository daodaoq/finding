import { useLocation, useNavigate } from 'react-router-dom';
import AppIcon from './AppIcon';
import { BOTTOM_NAV_ITEMS } from '../utils/constants';
import { useMessageStore } from '../store/messageStore';
import { useBridgeStore } from '../store/bridgeStore';
import './BottomNav.css';

interface Props { onCenterClick?: () => void; }

export default function BottomNav({ onCenterClick }: Props) {
  const location = useLocation();
  const navigate = useNavigate();
  const unreadCount = useMessageStore((s) => s.unreadCount);
  const bridgePending = useBridgeStore((s) => s.pendingCount);
  const isActive = (item: typeof BOTTOM_NAV_ITEMS[number]) => !item.isCenter && (item.key === 'home' ? location.pathname === '/' : location.pathname.startsWith(item.path));

  return <nav className="bottom-nav" aria-label="主导航">{BOTTOM_NAV_ITEMS.map((item) => {
    const count = item.key === 'messages' ? unreadCount : item.key === 'bridge' ? bridgePending : 0;
    return <button key={item.key} className={`nav-item ${item.isCenter ? 'nav-item--center' : ''} ${isActive(item) ? 'nav-item--active' : ''}`} onClick={() => item.isCenter ? onCenterClick?.() : navigate(item.path)}>
      <span className="nav-icon"><AppIcon name={item.icon} size={item.isCenter ? 18 : 20} /></span>
      <span className="nav-label">{item.label}{count > 0 && <sup className="nav-badge">{count > 99 ? '99+' : count}</sup>}</span>
    </button>;
  })}</nav>;
}
