import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { messageApi } from '../../api/message';
import { chatApi } from '../../api/chat';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import LoginModal from '../../components/LoginModal';
import { useMessageStore } from '../../store/messageStore';
import { useWebSocket } from '../../hooks/useWebSocket';
import { useAuthStore } from '../../store/authStore';
import type { Conversation } from '../../types/message';
import './index.css';

export default function MessagesPage() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [showLogin, setShowLogin] = useState(false);
  const navigate = useNavigate();
  const unreadCount = useMessageStore((s) => s.unreadCount);
  const setUnreadCount = useMessageStore((s) => s.setUnreadCount);
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);

  // WebSocket 实时接收新消息
  useWebSocket(useCallback((wsMsg) => {
    if (wsMsg.type === 'chat') {
      loadConversations(true);
      setUnreadCount(unreadCount + 1);
    }
  }, []));

  useEffect(() => {
    if (isLoggedIn) {
      loadConversations();
      loadUnreadCount();
    } else {
      setLoading(false);
    }
  }, [isLoggedIn]);

  const loadConversations = async (silent = false) => {
    if (!silent) setLoading(true);
    else setRefreshing(true);
    try {
      const res = await chatApi.listConversations();
      setConversations(res.data.data || []);
    } catch { /* ignore */ }
    finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const loadUnreadCount = async () => {
    try {
      const res = await messageApi.unreadCount();
      setUnreadCount(res.data.data.count);
    } catch { /* ignore */ }
  };

  // 点击通知行 → 跳转通知详情
  const handleNotificationClick = () => {
    navigate('/messages/notifications');
  };

  // 点击会话 → 进入聊天（传 roomId 给聊天页）
  const handleChatClick = (conv: Conversation) => {
    const name = encodeURIComponent(conv.targetNickname || `用户${conv.targetUserId}`);
    const avatar = encodeURIComponent(conv.targetAvatar || '');
    const roomId = conv.roomId || conv.id;
    navigate(`/messages/chat?userId=${conv.targetUserId}&name=${name}&avatar=${avatar}&roomId=${roomId}`);
  };

  // 下拉刷新
  const handleRefresh = async () => {
    await loadConversations(true);
    await loadUnreadCount();
  };

  return (
    <div className="messages-page">
      {/* Header */}
      <div className="msg-header">
        <h2 className="msg-header-title">互动消息</h2>
        <div className="msg-header-actions">
          <button className="header-action-btn" onClick={handleRefresh}>
            {refreshing ? '⏳' : '🔄'}
          </button>
        </div>
      </div>

      {/* 未登录提示 */}
      {!isLoggedIn && (
        <div className="msg-login-prompt" onClick={() => setShowLogin(true)}>
          <span className="msg-login-icon">🔒</span>
          <div>
            <p className="msg-login-title">登录后查看消息</p>
            <p className="msg-login-sub">登录后可查看互动通知和私聊消息</p>
          </div>
          <span className="msg-login-arrow">›</span>
        </div>
      )}

      {/* 已登录内容 */}
      {isLoggedIn && (
        <>
          {/* 通知浓缩行 */}
          <div className="notify-condensed" onClick={handleNotificationClick}>
            <div className="notify-icon-wrap">
              <span className="notify-icon">🔔</span>
              {unreadCount > 0 && <span className="notify-badge">{unreadCount}</span>}
            </div>
            <div className="notify-info">
              <span className="notify-label">互动通知</span>
              <span className="notify-sublabel">
                {unreadCount > 0 ? `${unreadCount}条未读` : '暂无新通知'}
              </span>
            </div>
            <span className="notify-arrow">›</span>
          </div>

          {/* 分隔线 */}
          <div className="section-divider">
            <span className="divider-text">私 聊</span>
          </div>

          {/* 会话列表 */}
          <div className="chat-conv-list">
            {loading && <><LoadingSkeleton /><LoadingSkeleton /></>}
            {!loading && conversations.map((conv) => (
              <div key={conv.id} className="chat-conv-item" onClick={() => handleChatClick(conv)}>
                <div className="conv-avatar">
                  {conv.targetAvatar ? <img src={conv.targetAvatar} alt="" /> : <span>👤</span>}
                </div>
                <div className="conv-info">
                  <div className="conv-top">
                    <span className="conv-name">{conv.targetNickname || `用户${conv.targetUserId}`}</span>
                    <span className="conv-time">
                      {conv.lastMessageAt ? formatConvTime(conv.lastMessageAt) : ''}
                    </span>
                  </div>
                  <div className="conv-bottom">
                    <span className="conv-preview">{conv.lastMessage || '暂无消息'}</span>
                    {conv.unreadCount > 0 && <span className="conv-badge">{conv.unreadCount}</span>}
                  </div>
                </div>
              </div>
            ))}
            {!loading && conversations.length === 0 && <EmptyState icon="💬" message="暂无会话" />}
          </div>
        </>
      )}

      {/* 登录弹窗 */}
      <LoginModal visible={showLogin} onClose={() => setShowLogin(false)}
        onSuccess={() => { setShowLogin(false); window.location.reload(); }} />
    </div>
  );
}

function formatConvTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
  if (diff < 172800000) return '昨天';
  return `${d.getMonth() + 1}/${d.getDate()}`;
}
