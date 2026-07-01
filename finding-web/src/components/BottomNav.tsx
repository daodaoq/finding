import { useLocation, useNavigate } from 'react-router-dom';
import { BOTTOM_NAV_ITEMS } from '../utils/constants';
import './BottomNav.css';

interface Props {
  onCenterClick?: () => void;
}

export default function BottomNav({ onCenterClick }: Props) {
  const location = useLocation();
  const navigate = useNavigate();

  const isActive = (item: typeof BOTTOM_NAV_ITEMS[number]) => {
    if (item.isCenter) return false;
    if (item.key === 'home') return location.pathname === '/';
    return location.pathname.startsWith(item.path);
  };

  return (
    <nav className="bottom-nav">
      {BOTTOM_NAV_ITEMS.map((item) => (
        <button
          key={item.key}
          className={`nav-item ${item.isCenter ? 'nav-item--center' : ''} ${
            isActive(item) ? 'nav-item--active' : ''
          }`}
          onClick={() => {
            if (item.isCenter) {
              onCenterClick?.();
            } else {
              navigate(item.path);
            }
          }}
        >
          <span className="nav-icon">{item.icon}</span>
          {!item.isCenter && <span className="nav-label">{item.label}</span>}
        </button>
      ))}
    </nav>
  );
}
