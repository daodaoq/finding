import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import './index.css';

export default function MinePage() {
  const user = useAuthStore((s) => s.user);
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="mine-page">
      {/* Top section */}
      <div className="mine-top">
        {isLoggedIn ? (
          <h2 className="mine-title">{user?.nickname}</h2>
        ) : (
          <>
            <h2 className="mine-title">登录 Finding</h2>
            <p className="mine-subtitle">登录后可查看个人信息、管理搭子、查看消息</p>
            <button className="login-btn" onClick={() => navigate('/login')}>
              手机号登录 / 注册
            </button>
          </>
        )}
        {isLoggedIn && user?.realNameVerified !== 2 && (
          <div className="verify-banner" onClick={() => navigate('/mine/verify')}>
            ⚠️ 请完成学生实名认证以使用全部功能
          </div>
        )}
      </div>

      {/* Quick actions row */}
      <div className="mine-quick-row">
        <button className="mine-quick-btn">❤️ 我的点赞</button>
        <button className="mine-quick-btn">👑 VIP注册</button>
        <button className="mine-quick-btn">💰 充值</button>
      </div>

      {/* Second row */}
      <div className="mine-quick-row">
        <button className="mine-quick-btn">👍 我的点赞</button>
        <button className="mine-quick-btn">📅 我的预约</button>
        <button className="mine-quick-btn">⭕ 我的圈子</button>
      </div>

      {/* Menu grid - 2 columns */}
      <div className="mine-menu-grid">
        <div className="mine-menu-col">
          <div className="menu-item" onClick={() => navigate('/mate')}>
            <span className="menu-icon">👫</span>
            <div className="menu-info">
              <span className="menu-label">我的搭子</span>
              <span className="menu-sub">我的朋友们</span>
            </div>
          </div>
          <div className="menu-item" onClick={() => navigate('/messages')}>
            <span className="menu-icon">💬</span>
            <div className="menu-info">
              <span className="menu-label">消息中心</span>
              <span className="menu-sub">互动消息</span>
            </div>
          </div>
          <div className="menu-item">
            <span className="menu-icon">🎧</span>
            <div className="menu-info">
              <span className="menu-label">客服中心</span>
              <span className="menu-sub">帮助与反馈</span>
            </div>
          </div>
        </div>
        <div className="mine-menu-col">
          <div className="menu-item">
            <span className="menu-icon">📋</span>
            <div className="menu-info">
              <span className="menu-label">搭子订单</span>
              <span className="menu-sub">我预约成功的搭子</span>
            </div>
          </div>
          <div className="menu-item">
            <span className="menu-icon">⚙️</span>
            <div className="menu-info">
              <span className="menu-label">搭子管理</span>
              <span className="menu-sub">我发布的所有邀约</span>
            </div>
          </div>
          <div className="menu-item">
            <span className="menu-icon">ℹ️</span>
            <div className="menu-info">
              <span className="menu-label">关于我们</span>
              <span className="menu-sub">Finding团队</span>
            </div>
          </div>
        </div>
      </div>

      {/* Logout */}
      {isLoggedIn && (
        <button className="logout-btn" onClick={handleLogout}>
          退出登录
        </button>
      )}
    </div>
  );
}
