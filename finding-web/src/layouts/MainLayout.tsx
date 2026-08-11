import { useState, useEffect, useLayoutEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import BottomNav from '../components/BottomNav';
import CreateActionSheet from '../components/CreateActionSheet';
import ToastContainer, { showToast } from '../components/Toast';
import InfoShareModal from '../components/InfoShareModal';
import AnnouncementModal, { getLastSeenAnnouncementId } from '../components/AnnouncementModal';
import type { AnnouncementData } from '../components/AnnouncementModal';
import BanModal from '../components/BanModal';
import { useAuthStore } from '../store/authStore';
import { useMessageStore } from '../store/messageStore';
import { useBridgeStore } from '../store/bridgeStore';
import { useInfoShareStore } from '../store/infoShareStore';
import { useWebSocket } from '../hooks/useWebSocket';
import { authApi } from '../api/auth';
import { bridgeApi } from '../api/bridge';
import { homeApi } from '../api/home';
import './MainLayout.css';

export default function MainLayout() {
  const [showCreate, setShowCreate] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const logout = useAuthStore((s) => s.logout);
  const setUnreadCount = useMessageStore((s) => s.setUnreadCount);
  const refreshUnread = useMessageStore((s) => s.refreshTotal);
  const setBridgePending = useBridgeStore((s) => s.setPendingCount);
  const setInfoSharePrompt = useInfoShareStore((s) => s.setPrompt);
  const bumpInfoShare = useInfoShareStore((s) => s.bump);

  // Router navigation should always start at the top. Some primary pages own
  // their scroll container, so resetting window.scrollY alone is insufficient.
  useEffect(() => {
    const previousRestoration = window.history.scrollRestoration;
    window.history.scrollRestoration = 'manual';
    return () => {
      window.history.scrollRestoration = previousRestoration;
    };
  }, []);

  useLayoutEffect(() => {
    const resetScroll = () => {
      window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
      document.documentElement.scrollTop = 0;
      document.body.scrollTop = 0;
      document
        .querySelectorAll<HTMLElement>('.main-content, .home-page, .square-page, .mate-page, .bridge-page')
        .forEach((element) => {
          element.scrollTop = 0;
          element.scrollLeft = 0;
        });
    };

    resetScroll();
    const frame = requestAnimationFrame(resetScroll);
    return () => cancelAnimationFrame(frame);
  }, [location.pathname, location.search, location.hash]);

  // 系统公告:全部未读公告(WS 推送 + 启动补拉),一次滚动展示
  const [announcements, setAnnouncements] = useState<AnnouncementData[]>([]);
  // 封禁提示(收到 ban 推送后弹出,关闭即强制退出)
  const [banMessage, setBanMessage] = useState<string | null>(null);

  const enqueueAnnouncement = (a: AnnouncementData) => {
    setAnnouncements((prev) => prev.some((x) => x.id === a.id) ? prev : [...prev, a]);
  };

  const handleBanClose = () => {
    setBanMessage(null);
    logout();
    navigate('/login');
  };

  // 全局 WebSocket:实时接收「信息互换」请求/结果 + 「系统公告」+ 「封禁」推送(登录后连接)
  useWebSocket((msg) => {
    if (msg.type === 'ban') {
      setBanMessage(msg.content || '你已被封禁');
      return;
    }
    if (msg.type === 'system_announcement') {
      enqueueAnnouncement({
        id: msg.messageId,
        title: msg.title || '系统公告',
        content: msg.content || '',
        createdAt: msg.timestamp ? new Date(msg.timestamp).toISOString() : undefined,
      });
      return;
    }
    // 收到新通知/私聊/群聊消息 → 刷新汇总角标(通知+私聊+群聊)
    if (msg.type === 'new_notification' || msg.type === 'chat' || msg.type === 'group_chat') {
      refreshUnread();
      return;
    }
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

  // 启动时补拉「已读之后」的全部未读公告(覆盖离线期间发布的多条)
  useEffect(() => {
    homeApi.announcements(getLastSeenAnnouncementId())
      .then((res) => {
        (res.data.data || []).forEach((a) =>
          enqueueAnnouncement({ id: a.id, title: a.title || '系统公告', content: a.content || '', createdAt: a.createdAt }));
      })
      .catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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

  // 全局角标计数：通知+私聊+群聊汇总未读 + 鹊桥待处理申请数
  useEffect(() => {
    if (isLoggedIn) {
      refreshUnread();
      bridgeApi.receivedPendingCount().then((res) => setBridgePending(res.data.data?.count ?? 0)).catch(() => {});
    } else {
      setUnreadCount(0);
      setBridgePending(0);
    }
  }, [isLoggedIn, setUnreadCount, setBridgePending, refreshUnread]);

  // 回到前台时刷新角标(读过的私聊/群聊/通知后回来能及时减)
  useEffect(() => {
    const onFocus = () => {
      if (isLoggedIn) refreshUnread();
    };
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, [isLoggedIn, refreshUnread]);

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
      <AnnouncementModal announcements={announcements} onClose={() => setAnnouncements([])} />
      <BanModal message={banMessage} onClose={handleBanClose} />
    </div>
  );
}
