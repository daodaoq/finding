import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { messageApi } from '../../api/message';
import MessageItem from '../../components/MessageItem';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { useMessageStore } from '../../store/messageStore';
import type { Message } from '../../types/message';
import './index.css';

export default function MessagesPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const setUnreadCount = useMessageStore((s) => s.setUnreadCount);

  useEffect(() => {
    loadMessages();
    loadUnreadCount();
  }, []);

  const loadMessages = async () => {
    setLoading(true);
    try {
      const res = await messageApi.list({ page: 1, size: 50 });
      setMessages(res.data.data.records);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const loadUnreadCount = async () => {
    try {
      const res = await messageApi.unreadCount();
      setUnreadCount(res.data.data.count);
    } catch { /* ignore */ }
  };

  const handleClick = async (msg: Message) => {
    if (!msg.isRead) {
      try {
        await messageApi.markRead(msg.id);
        setMessages((prev) => prev.map((m) =>
          m.id === msg.id ? { ...m, isRead: 1 } : m));
        loadUnreadCount();
      } catch { /* ignore */ }
    }
    // Navigate based on type
    if (msg.type === 'like' || msg.type === 'comment') {
      navigate(`/square/post/${msg.relatedId}`);
    }
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

      {/* Action buttons */}
      <div className="msg-action-row">
        <button className="msg-action-btn">扫一扫</button>
        <button className="msg-action-btn">添加好友</button>
        <button className="msg-action-btn">发起群聊</button>
      </div>

      {/* Message list */}
      <div className="msg-list">
        {messages.map((msg) => (
          <MessageItem key={msg.id} message={msg} onClick={handleClick} />
        ))}
        {loading && <LoadingSkeleton />}
        {!loading && messages.length === 0 && <EmptyState message="暂无消息" />}
      </div>
    </div>
  );
}
