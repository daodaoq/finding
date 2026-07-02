import { useEffect, useState, useRef } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { groupChatApi } from '../../api/groupChat';
import { useAuthStore } from '../../store/authStore';
import { showToast } from '../../components/Toast';
import ChatBubble from '../../components/ChatBubble';
import ChatInputBar from '../../components/ChatInputBar';
import type { GroupMessage } from '../../types/groupChat';
import '../Chat/index.css';

export default function GroupChatPage() {
  const { id } = useParams<{ id: string }>();
  const groupId = Number(id);
  const [searchParams] = useSearchParams();
  const groupName = searchParams.get('name') || '群聊';

  const [messages, setMessages] = useState<GroupMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const msgListRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    const el = msgListRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  };

  useEffect(() => { loadMessages(); }, [groupId]);

  const loadMessages = async () => {
    try {
      setLoading(true);
      const res = await groupChatApi.getMessageHistory(groupId);
      setMessages(res.data.data.records || []);
    } catch { /**/ }
    finally { setLoading(false); }
  };

  useEffect(() => {
    scrollToBottom();
    const t = setTimeout(scrollToBottom, 300);
    return () => clearTimeout(t);
  }, [messages]);

  // TODO: 后续可接入 WebSocket 实时接收群消息
  // 目前每次进页面重新加载，也可以加轮询

  const handleSend = async (content: string, messageType = 'text') => {
    if (!user || !content) return;
    const tempId = Date.now();
    const newMsg: GroupMessage = {
      id: tempId,
      groupId,
      fromUserId: user.id,
      fromUserNickname: user.nickname || '我',
      fromUserAvatar: user.avatar || '',
      content,
      messageType,
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, newMsg]);

    try {
      await groupChatApi.sendMessage(groupId, content, messageType);
    } catch (e: any) {
      setMessages((prev) => prev.filter((m) => m.id !== tempId));
      showToast(e?.message || '发送失败');
    }
  };

  if (loading) {
    return (
      <div className="chat-page">
        <div className="chat-header">
          <button className="back-btn" onClick={() => navigate(-1)}>←</button>
          <span>{groupName}</span>
        </div>
        <div className="chat-loading">加载中...</div>
      </div>
    );
  }

  return (
    <div className="chat-page">
      <div className="chat-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <span className="chat-header-name" style={{ cursor: 'pointer' }}
          onClick={() => navigate(`/messages/group-chat/${groupId}/info?name=${encodeURIComponent(groupName)}`)}>
          {groupName}
        </span>
      </div>

      <div className="chat-messages" ref={msgListRef}>
        {messages.map((msg, i) => {
          const prevMsg = i > 0 ? messages[i - 1] : null;
          const showTimeSep = !prevMsg ||
            (new Date(msg.createdAt).getTime() - new Date(prevMsg.createdAt).getTime()) > 10 * 60 * 1000;
          return (
            <div key={msg.id}>
              {showTimeSep && (
                <div className="chat-time-sep"><span>{formatChatTime(msg.createdAt)}</span></div>
              )}
              <ChatBubble
                message={msg}
                isMine={msg.fromUserId === user?.id}
                avatar={msg.fromUserAvatar}
                nickname={msg.fromUserNickname}
              />
            </div>
          );
        })}
        <div ref={() => {}} /* scroll anchor */ />
      </div>

      <ChatInputBar onSend={handleSend} />
    </div>
  );
}

function formatChatTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today.getTime() - 86400000);
  const date = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const time = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  if (date.getTime() === today.getTime()) return `今天 ${time}`;
  if (date.getTime() === yesterday.getTime()) return `昨天 ${time}`;
  return `${d.getMonth() + 1}/${d.getDate()} ${time}`;
}
