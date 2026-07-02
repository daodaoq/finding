import { useEffect, useState, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { chatApi } from '../../api/chat';
import { useWebSocket } from '../../hooks/useWebSocket';
import { useAuthStore } from '../../store/authStore';
import { showToast } from '../../components/Toast';
import ChatBubble from '../../components/ChatBubble';
import ChatInputBar from '../../components/ChatInputBar';
import type { Conversation } from '../../types/message';
import './index.css';

interface ChatMessage {
  id: number;
  fromUserId: number;
  toUserId: number;
  content: string;
  messageType: string;
  isRead: number;
  createdAt: string;
}

export default function ChatDetailPage() {
  const [searchParams] = useSearchParams();
  const targetUserId = Number(searchParams.get('userId'));
  const targetNickname = searchParams.get('name') || '聊天';
  const targetAvatar = searchParams.get('avatar') || '';

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [conversation, setConversation] = useState<Conversation | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const msgListRef = useRef<HTMLDivElement>(null);

  // WebSocket 实时消息处理
  const { sendMessage } = useWebSocket((wsMsg) => {
    if (wsMsg.type === 'chat' && wsMsg.fromUserId === targetUserId && wsMsg.messageId) {
      // 按 ID 去重：避免消息历史加载完后又收到同一条 WebSocket 推送
      setMessages((prev) => {
        if (prev.some((m) => m.id === wsMsg.messageId)) return prev;
        return [...prev, {
          id: wsMsg.messageId,
          fromUserId: wsMsg.fromUserId,
          toUserId: wsMsg.toUserId,
          content: wsMsg.content,
          messageType: wsMsg.messageType || 'text',
          isRead: 0,
          createdAt: new Date().toISOString(),
        }];
      });
    }
  });

  // 初始化：获取或创建会话 + 加载消息历史
  useEffect(() => {
    initConversation();
  }, [targetUserId]);

  const initConversation = async () => {
    try {
      setLoading(true);
      // 创建或获取会话
      const convRes = await chatApi.getOrCreateConversation(targetUserId);
      const conv = convRes.data.data;
      setConversation(conv);
      // 使用 roomId 加载消息历史
      const roomId = conv.roomId || conv.id;
      const msgRes = await chatApi.getMessageHistory(roomId);
      const records = (msgRes.data.data.records || []).map((r: any) => ({
        id: r.id,
        fromUserId: r.fromUserId,
        toUserId: r.toUserId,
        content: r.content,
        messageType: r.messageType || 'text',
        isRead: r.isRead,
        createdAt: r.createdAt,
      }));
      setMessages(records);
    } catch (e) {
      console.error('初始化会话失败', e);
    } finally {
      setLoading(false);
    }
  };

  // 自动滚动到底部（含图片加载后的二次矫正）
  const scrollToBottom = () => {
    const el = msgListRef.current;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  };

  useEffect(() => {
    scrollToBottom();
    // 图片加载会撑开高度，延迟再滚一次确保到底
    const t = setTimeout(scrollToBottom, 300);
    return () => clearTimeout(t);
  }, [messages]);

  // 发送消息
  const handleSend = async (content: string, messageType = 'text') => {
    if (!user || !conversation) return;
    const newMsg: ChatMessage = {
      id: Date.now(), // 临时 ID
      fromUserId: user.id,
      toUserId: targetUserId,
      content,
      messageType,
      isRead: 0,
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, newMsg]);

    try {
      await chatApi.sendMessage({ toUserId: targetUserId, content, messageType });
    } catch (e: any) {
      setMessages((prev) => prev.filter((m) => m.id !== newMsg.id));
      showToast(e?.message || '发送失败，请稍后重试');
    }
  };

  if (loading) {
    return (
      <div className="chat-page">
        <div className="chat-header">
          <button className="back-btn" onClick={() => navigate(-1)}>←</button>
          <span>{targetNickname}</span>
        </div>
        <div className="chat-loading">加载中...</div>
      </div>
    );
  }

  return (
    <div className="chat-page">
      {/* 顶部栏 */}
      <div className="chat-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <div className="chat-avatar-sm">
          {targetAvatar ? <img src={targetAvatar} alt="" /> : <span>👤</span>}
        </div>
        <span className="chat-header-name">{targetNickname}</span>
      </div>

      {/* 消息列表 */}
      <div className="chat-messages" ref={msgListRef}>
        {messages.map((msg, i) => {
          const prevMsg = i > 0 ? messages[i - 1] : null;
          const showTimeSep = !prevMsg ||
            (new Date(msg.createdAt).getTime() - new Date(prevMsg.createdAt).getTime()) > 10 * 60 * 1000;
          return (
            <div key={msg.id}>
              {showTimeSep && (
                <div className="chat-time-sep">
                  <span>{formatChatTime(msg.createdAt)}</span>
                </div>
              )}
              <ChatBubble
                message={msg}
                isMine={msg.fromUserId === user?.id}
                avatar={msg.fromUserId === user?.id ? (user?.avatar || '') : targetAvatar}
                nickname={msg.fromUserId === user?.id ? (user?.nickname || '我') : targetNickname}
              />
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* 输入栏 */}
      <ChatInputBar onSend={handleSend} onUploading={setUploading} />
    </div>
  );
}

/** 聊天时间分隔线格式：今天 HH:mm / 昨天 HH:mm / MM/DD HH:mm */
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
