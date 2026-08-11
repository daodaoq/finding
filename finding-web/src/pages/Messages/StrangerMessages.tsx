import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { chatApi } from '../../api/chat';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import AppIcon from '../../components/AppIcon';
import { showToast } from '../../components/Toast';
import type { StrangerMessage } from '../../types/message';
import './index.css';

export default function StrangerMessagesPage() {
  const [messages, setMessages] = useState<StrangerMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [acceptingId, setAcceptingId] = useState<number | null>(null);
  const navigate = useNavigate();

  const load = async () => {
    try {
      const res = await chatApi.listStrangerMessages();
      setMessages(res.data.data || []);
    } catch {
      showToast('加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleAccept = async (m: StrangerMessage) => {
    setAcceptingId(m.id);
    try {
      await chatApi.acceptStrangerMessage(m.id);
      showToast('已确认聊天，会话已移至消息列表');
      await load();
    } catch (e: any) {
      showToast(e?.message || '操作失败');
    } finally {
      setAcceptingId(null);
    }
  };

  const formatTime = (dateStr?: string): string => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="messages-page sm-page">
      <header className="msg-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h2 className="msg-header-title">陌生人消息</h2>
      </header>

      <p className="sm-hint">对方确认聊天后，会变成正式会话移到消息列表</p>

      <section className="sm-list">
        {loading && <><LoadingSkeleton /><LoadingSkeleton /></>}
        {!loading && messages.map((m) => (
          <div key={m.id} className="sm-item">
            <div className="conv-avatar">
              {m.otherAvatar ? <img src={m.otherAvatar} alt="" /> : <AppIcon name="user" size={21} />}
            </div>
            <div className="sm-info">
              <div className="conv-top">
                <b>{m.otherNickname || `用户${m.otherUserId}`}</b>
                <small>{formatTime(m.createdAt)}</small>
              </div>
              <div className="sm-content">{m.content}</div>
            </div>
            <div className="sm-action">
              {m.direction === 'received' ? (
                <button className="sm-accept" onClick={() => handleAccept(m)} disabled={acceptingId === m.id}>
                  {acceptingId === m.id ? '确认中...' : '确认聊天'}
                </button>
              ) : (
                <span className="sm-waiting">等待对方确认</span>
              )}
            </div>
          </div>
        ))}
        {!loading && messages.length === 0 && (
          <EmptyState message="还没有陌生人消息" />
        )}
      </section>
    </div>
  );
}
