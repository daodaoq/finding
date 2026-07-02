import { useState, useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import BottomNav from '../components/BottomNav';
import CreateActionSheet from '../components/CreateActionSheet';
import ToastContainer from '../components/Toast';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/auth';
import './MainLayout.css';

export default function MainLayout() {
  const [showCreate, setShowCreate] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);

  // 用户信息水合：登录态有效但 user 为空时，自动拉取
  useEffect(() => {
    if (isLoggedIn && !user) {
      authApi.getMe().then((res) => {
        setUser(res.data.data);
      }).catch(() => {
        // token 可能已过期，忽略
      });
    }
  }, [isLoggedIn, user]);

  // 监听 BottomNav 的中间的"+"点击
  const handleCreatePost = () => navigate('/create-post');
  const handleCreateMate = () => navigate('/create-mate');

  return (
    <div className="main-layout">
      <div className="main-content">
        <Outlet context={{ openCreateSheet: () => setShowCreate(true) }} />
      </div>
      <BottomNav onCenterClick={() => setShowCreate(true)} />
      <CreateActionSheet
        visible={showCreate}
        onClose={() => setShowCreate(false)}
        onCreatePost={handleCreatePost}
        onCreateMate={handleCreateMate}
      />
      <ToastContainer />
    </div>
  );
}
