import { useEffect, useState, useRef } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { groupChatApi } from '../../api/groupChat';
import { useAuthStore } from '../../store/authStore';
import { showToast } from '../../components/Toast';
import ChatInputBar from '../../components/ChatInputBar';
import ChatHeader from '../Chat/components/ChatHeader';
import MessageList from '../Chat/components/MessageList';
import type { GroupMessage } from '../../types/groupChat';
import '../Chat/index.css';

export default function GroupChatPage() {
  const { id } = useParams<{ id: string }>();
  const groupId = Number(id);
  const [searchParams] = useSearchParams();
  const groupName = searchParams.get('name') || '群聊';

  const [messages, setMessages] = useState<GroupMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const msgListRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    const el = msgListRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  };

  useEffect(() => {
    if (groupId && !isNaN(groupId)) {
      loadMessages();
    } else {
      setLoading(false);
      setLoadError(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [groupId]);

  const loadMessages = async () => {
    try {
      setLoading(true);
      setLoadError(false);
      const res = await groupChatApi.getMessageHistory(groupId);
      setMessages(res.data.data.records || []);
    } catch (e) {
      setLoadError(true);
    }
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
      <ChatHeader
        title={groupName}
        onBack={() => navigate(-1)}
        right={(
          <button
            className="chat-info-btn"
            style={{
              marginLeft: 'auto', background: 'none', border: 'none', fontSize: 20, cursor: 'pointer', padding: '0 4px',
            }}
            onClick={() => navigate(`/messages/group-chat/${groupId}/info?name=${encodeURIComponent(groupName)}`)}
          >
            ℹ️
          </button>
        )}
      />

      <MessageList
        messages={messages}
        currentUserId={user?.id}
        avatarOf={(msg) => (msg as GroupMessage).fromUserAvatar}
        nicknameOf={(msg) => (msg as GroupMessage).fromUserNickname}
        listRef={msgListRef}
        errorNode={loadError ? (
          <div style={{ textAlign: 'center', padding: 40, color: '#999' }}>
            ⚠️ 加载失败，请确认已加入该群聊
          </div>
        ) : undefined}
        emptyNode={!loadError && messages.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 40, color: '#ccc' }}>
            暂无消息，发送第一条消息吧
          </div>
        ) : undefined}
      />

      <ChatInputBar onSend={handleSend} />
    </div>
  );
}