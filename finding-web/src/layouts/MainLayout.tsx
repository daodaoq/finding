import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import BottomNav from '../components/BottomNav';
import CreateActionSheet from '../components/CreateActionSheet';
import ToastContainer from '../components/Toast';
import './MainLayout.css';

export default function MainLayout() {
  const [showCreate, setShowCreate] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  // 监听 BottomNav 的中间的"+"点击 → 通过 location.state 传递
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
