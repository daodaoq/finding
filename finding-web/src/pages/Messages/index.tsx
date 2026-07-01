import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { messageApi } from '../../api/message';
import { chatApi } from '../../api/chat';
import MessageItem from '../../components/MessageItem';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { useMessageStore } from '../../store/messageStore';
import type { Message, Conversation } from '../../types/message';
import './index.css';

export default function MessagesPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'notifications' | 'chats'>('notifications');
  const navigate = useNavigate();
  const setUnreadCount = useMessageStore((s) => s.setUnreadCount);

  useEffect(() => {
    loadMessages();
    loadConversations();
    loadUnreadCount();
  }, []);

  const loadMessages = async () => {
    try {
      const res = await messageApi.list({ page: 1, size: 50 });
      setMessages(res.data.data.records);
    } catch { /* ignore */ }
  };

  const loadConversations = async () => {
    setLoading(true);
    try {
      const res = await chatApi.listConversations();
      setConversations(res.data.data);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const loadUnreadCount = async () => {
    try {
      const res = await messageApi.unreadCount();
      setUnreadCount(res.data.data.count);
    } catch { /* ignore */ }
  };

  const handleNotificationClick = async (msg: Message) => {
    if (!msg.isRead) {
      try {
        await messageApi.markRead(msg.id);
        setMessages((prev) => prev.map((m) =>
          m.id === msg.id ? { ...m, isRead: 1 } : m));
        loadUnreadCount();
      } catch { /* ignore */ }
    }
    if (msg.type === 'like' || msg.type === 'comment') {
      navigate(`/square/post/${msg.relatedId}`);
    }
    if (msg.type === 'mate_request' || msg.type === 'mate_accepted') {
      navigate(`/mate/${msg.relatedId}`);
    }
  };

  const handleChatClick = (conv: Conversation) => {
    navigate(`/messages/chat?userId=${conv.targetUserId}&name=${encodeURIComponent(conv.targetNickname || '')}&avatar=${encodeURIComponent(conv.targetAvatar || '')}`);
  };

  return (
    <div className="messages-page">
      {/* Header */}
      <div className="msg-header">
        <h2 className="msg-header-title">互动消息</h2>
        <div className="msg-header-actions">
          <button>📷</button>
          <button>👥➕</button>
          <button className="plus-btn">＋</button>
        </div>
      </div>

      {/* 切换标签 */}
      <div className="msg-tabs">
        <button
          className={`msg-tab ${activeTab === 'notifications' ? 'active' : ''}`}
          onClick={() => setActiveTab('notifications')}
        >
          通知
        </button>
        <button
          className={`msg-tab ${activeTab === 'chats' ? 'active' : ''}`}
          onClick={() => setActiveTab('chats')}
        >
          私聊
        </button>
      </div>

      {/* 通知列表 */}
      {activeTab === 'notifications' && (
        <div className="msg-list">
          {messages.map((msg) => (
            <MessageItem key={msg.id} message={msg} onClick={handleNotificationClick} />
          ))}
          {messages.length === 0 && <EmptyState message="暂无通知" />}
        </div>
      )}

      {/* 会话列表 */}
      {activeTab === 'chats' && (
        <div className="msg-list">
          {loading && <LoadingSkeleton />}
          {!loading && conversations.map((conv) => (
            <div key={conv.id} className="chat-conv-item" onClick={() => handleChatClick(conv)}>
              <div className="conv-avatar">
                {conv.targetAvatar ? <img src={conv.targetAvatar} alt="" /> : <span>👤</span>}
              </div>
              <div className="conv-info">
                <div className="conv-name">
                  {conv.targetNickname || `用户${conv.targetUserId}`}
                  {conv.unreadCount > 0 && <span className="conv-badge">{conv.unreadCount}</span>}
                </div>
                <div className="conv-preview">{conv.lastMessage || '暂无消息'}</div>
              </div>
              <div className="conv-time">
                {conv.lastMessageAt ? formatConvTime(conv.lastMessageAt) : ''}
              </div>
            </div>
          ))}
          {!loading && conversations.length === 0 && <EmptyState message="暂无会话" />}
        </div>
      )}
    </div>
  );
}

function formatConvTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 86400000) {
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }
  return `${d.getMonth() + 1}/${d.getDate()}`;
}
