import { useEffect, useState, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { chatApi } from '../../api/chat';
import { useWebSocket } from '../../hooks/useWebSocket';
import { useAuthStore } from '../../store/authStore';
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
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // WebSocket 实时消息处理
  const { sendMessage } = useWebSocket((wsMsg) => {
    if (wsMsg.type === 'chat' && wsMsg.fromUserId === targetUserId) {
      // 收到新消息，追加到列表
      setMessages((prev) => [...prev, {
        id: wsMsg.messageId || Date.now(),
        fromUserId: wsMsg.fromUserId,
        toUserId: wsMsg.toUserId,
        content: wsMsg.content,
        messageType: wsMsg.messageType || 'text',
        isRead: 0,
        createdAt: new Date().toISOString(),
      }]);
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

  // 自动滚动到底部
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // 发送消息
  const handleSend = async (content: string) => {
    if (!user || !conversation) return;
    const newMsg: ChatMessage = {
      id: Date.now(), // 临时 ID
      fromUserId: user.id,
      toUserId: targetUserId,
      content,
      messageType: 'text',
      isRead: 0,
      createdAt: new Date().toISOString(),
    };

    // 乐观更新：立即显示
    setMessages((prev) => [...prev, newMsg]);

    // 通过 REST API 发送（同时会触发 WebSocket 推送给对方）
    try {
      await chatApi.sendMessage({ toUserId: targetUserId, content, messageType: 'text' });
    } catch (e) {
      console.error('发送消息失败', e);
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
      <div className="chat-messages">
        {messages.map((msg) => (
          <ChatBubble
            key={msg.id}
            message={msg}
            isMine={msg.fromUserId === user?.id}
            avatar={msg.fromUserId === user?.id ? (user?.avatar || '') : targetAvatar}
            nickname={msg.fromUserId === user?.id ? (user?.nickname || '我') : targetNickname}
          />
        ))}
        <div ref={messagesEndRef} />
      </div>

      {/* 输入栏 */}
      <ChatInputBar onSend={handleSend} />
    </div>
  );
}
