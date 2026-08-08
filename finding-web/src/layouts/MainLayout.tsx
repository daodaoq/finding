import { useState, useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import BottomNav from '../components/BottomNav';
import CreateActionSheet from '../components/CreateActionSheet';
import ToastContainer, { showToast } from '../components/Toast';
import InfoShareModal from '../components/InfoShareModal';
import { useAuthStore } from '../store/authStore';
import { useMessageStore } from '../store/messageStore';
import { useBridgeStore } from '../store/bridgeStore';
import { useInfoShareStore } from '../store/infoShareStore';
import { useWebSocket } from '../hooks/useWebSocket';
import { authApi } from '../api/auth';
import { messageApi } from '../api/message';
import { bridgeApi } from '../api/bridge';
import './MainLayout.css';

export default function MainLayout() {
  const [showCreate, setShowCreate] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const setUnreadCount = useMessageStore((s) => s.setUnreadCount);
  const setBridgePending = useBridgeStore((s) => s.setPendingCount);
  const setInfoSharePrompt = useInfoShareStore((s) => s.setPrompt);
  const bumpInfoShare = useInfoShareStore((s) => s.bump);

  // 全局 WebSocket:实时接收「信息互换」请求/结果(登录后连接)
  useWebSocket((msg) => {
    if (msg.type !== 'info_share') return;
    if (msg.action === 'request') {
      setInfoSharePrompt({
        shareId: msg.messageId,
        fromUserId: msg.fromUserId,
        fromNickname: msg.content,
      });
    } else if (msg.action === 'approved' || msg.action === 'rejected') {
      showToast(msg.content);
      bumpInfoShare();
    }
  }, isLoggedIn);

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

  // 全局角标计数：消息未读数 + 鹊桥待处理申请数
  useEffect(() => {
    if (isLoggedIn) {
      messageApi.unreadCount().then((res) => setUnreadCount(res.data.data.count)).catch(() => {});
      bridgeApi.receivedPendingCount().then((res) => setBridgePending(res.data.data?.count ?? 0)).catch(() => {});
    } else {
      setUnreadCount(0);
      setBridgePending(0);
    }
  }, [isLoggedIn, setUnreadCount, setBridgePending]);

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
      <InfoShareModal />
    </div>
  );
}
