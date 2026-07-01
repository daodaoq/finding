import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { messageApi } from '../../api/message';
import MessageItem from '../../components/MessageItem';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { useMessageStore } from '../../store/messageStore';
import type { Message } from '../../types/message';
import './index.css';

export default function NotificationsPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const setUnreadCount = useMessageStore((s) => s.setUnreadCount);

  useEffect(() => {
    loadAll();
  }, []);

  const loadAll = async () => {
    setLoading(true);
    try {
      const [msgRes] = await Promise.all([
        messageApi.list({ page: 1, size: 100 }),
        messageApi.markAllRead(),
      ]);
      setMessages(msgRes.data.data.records);
      setUnreadCount(0);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const handleClick = async (msg: Message) => {
    // 根据类型跳转
    if (msg.type === 'like' || msg.type === 'comment') {
      navigate(`/square/post/${msg.relatedId}`);
    } else if (msg.type === 'mate_request' || msg.type === 'mate_accepted' || msg.type === 'mate_rejected') {
      navigate(`/mate/${msg.relatedId}`);
    } else if (msg.type === 'follow') {
      // 跳转到对方主页
      navigate(`/mine`);
    }
    // system 类型不做跳转
  };

  return (
    <div className="notifications-page">
      {/* Header */}
      <div className="notif-header">
        <button className="back-btn" onClick={() => navigate('/messages')}>←</button>
        <h2 className="notif-title">互动通知</h2>
        <div style={{ width: 40 }} />
      </div>

      {/* 消息列表 */}
      <div className="notif-list">
        {loading && <><LoadingSkeleton /><LoadingSkeleton /></>}
        {!loading && messages.map((msg) => (
          <MessageItem key={msg.id} message={msg} onClick={handleClick} />
        ))}
        {!loading && messages.length === 0 && (
          <EmptyState icon="🔔" message="暂无通知" />
        )}
      </div>
    </div>
  );
}
