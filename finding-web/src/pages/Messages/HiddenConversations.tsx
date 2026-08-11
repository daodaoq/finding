import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { chatApi } from '../../api/chat';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import AppIcon from '../../components/AppIcon';
import { showToast } from '../../components/Toast';
import { formatSessionTime } from '../../utils/format';
import type { Conversation } from '../../types/message';
import './index.css';

export default function HiddenConversationsPage() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loading, setLoading] = useState(true);
  const [restoringId, setRestoringId] = useState<number | null>(null);
  const navigate = useNavigate();

  const load = async () => {
    try {
      const res = await chatApi.listHiddenConversations();
      setConversations(res.data.data || []);
    } catch {
      showToast('加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleRestore = async (conv: Conversation) => {
    setRestoringId(conv.id);
    try {
      await chatApi.hideConversation(conv.roomId || conv.id, false);
      showToast('已恢复会话');
      await load();
    } catch (e: any) {
      showToast(e?.message || '恢复失败');
    } finally {
      setRestoringId(null);
    }
  };

  return (
    <div className="messages-page">
      <header className="msg-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h2 className="msg-header-title">隐藏的会话</h2>
      </header>

      <p className="sm-hint">隐藏的会话不在主列表显示，对方发新消息会自动恢复</p>

      <section className="sm-list">
        {loading && <><LoadingSkeleton /><LoadingSkeleton /></>}
        {!loading && conversations.map((conv) => (
          <div key={conv.id} className="sm-item">
            <div className="conv-avatar">
              {conv.targetAvatar ? <img src={conv.targetAvatar} alt="" /> : <AppIcon name="user" size={21} />}
            </div>
            <div className="sm-info">
              <div className="conv-top">
                <b>{conv.targetNickname || `用户${conv.targetUserId}`}</b>
                <small>{conv.lastMessageAt ? formatSessionTime(conv.lastMessageAt) : ''}</small>
              </div>
              <div className="sm-content">{conv.lastMessage || '暂无消息'}</div>
            </div>
            <div className="sm-action">
              <button className="sm-accept" onClick={() => handleRestore(conv)} disabled={restoringId === conv.id}>
                {restoringId === conv.id ? '恢复中...' : '恢复'}
              </button>
            </div>
          </div>
        ))}
        {!loading && conversations.length === 0 && (
          <EmptyState message="没有隐藏的会话" />
        )}
      </section>
    </div>
  );
}
