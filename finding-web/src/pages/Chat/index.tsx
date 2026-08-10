import { useEffect, useState, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { chatApi } from '../../api/chat';
import { bridgeApi } from '../../api/bridge';
import { useWebSocket } from '../../hooks/useWebSocket';
import { useAuthStore } from '../../store/authStore';
import { useMessageStore } from '../../store/messageStore';
import { useInfoShareStore } from '../../store/infoShareStore';
import { showToast } from '../../components/Toast';
import ChatInputBar from '../../components/ChatInputBar';
import AppIcon from '../../components/AppIcon';
import ConfirmDialog from '../../components/ConfirmDialog';
import ReportDialog from '../../components/ReportDialog';
import ChatHeader from './components/ChatHeader';
import ShareStatusTag from './components/ShareStatusTag';
import MessageList, { type MessageLike } from './components/MessageList';
import type { Conversation, ChatSettings } from '../../types/message';
import type { InfoShareStatus } from '../../types/resume';
import { resolveChatBg } from '../../utils/chatBackgrounds';
import './index.css';

interface ChatMessage extends MessageLike {
  toUserId: number;
  isRead: number;
}

export default function ChatDetailPage() {
  const [searchParams] = useSearchParams();
  const targetUserId = Number(searchParams.get('userId'));
  const targetNickname = searchParams.get('name') || '聊天';
  const targetAvatar = searchParams.get('avatar') || '';

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [conversation, setConversation] = useState<Conversation | null>(null);
  const [loading, setLoading] = useState(true);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [uploading, setUploading] = useState(false);
  // 信息互换按钮状态
  const [shareStatus, setShareStatus] = useState<InfoShareStatus['status']>('none');
  const [shareId, setShareId] = useState<number | null>(null);
  const [showShareConfirm, setShowShareConfirm] = useState(false);
  // 会话设置(聊天背景)
  const [chatSettings, setChatSettings] = useState<ChatSettings | null>(null);
  // 长按消息投诉
  const [reportTarget, setReportTarget] = useState<{
    targetType: string; targetId: number; roomId?: number; title: string;
  } | null>(null);
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const shareVersion = useInfoShareStore((s) => s.version);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const msgListRef = useRef<HTMLDivElement>(null);

  // WebSocket 实时消息处理
  const { sendMessage } = useWebSocket((wsMsg) => {
    if (wsMsg.type === 'message_recalled' && wsMsg.messageId) {
      setMessages((prev) => prev.map((m) =>
        m.id === wsMsg.messageId ? { ...m, isRecalled: 1, content: '该消息已撤回' } : m));
      return;
    }
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
        isRecalled: r.isRecalled,
        isRead: r.isRead,
        createdAt: r.createdAt,
      }));
      setMessages(records);
      // 进入会话已标记已读 → 刷新汇总角标
      useMessageStore.getState().refreshTotal();
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
    const t = setTimeout(scrollToBottom, 300);
    return () => clearTimeout(t);
  }, [messages]);

  // 拉取与对方的「信息互换」状态(header 按钮)；互换事件后通过全局 version 自动刷新
  useEffect(() => {
    if (!targetUserId || !user) return;
    bridgeApi.infoShareStatus(targetUserId)
      .then((res) => {
        setShareStatus(res.data.data.status);
        setShareId(res.data.data.shareId);
      })
      .catch(() => {});
  }, [targetUserId, user, shareVersion]);

  // 拉取会话设置(聊天背景)
  useEffect(() => {
    const roomId = conversation?.roomId || conversation?.id;
    if (!roomId) return;
    chatApi.getSettings(roomId).then((res) => setChatSettings(res.data.data)).catch(() => {});
  }, [conversation]);

  // 发起互换申请
  const handleRequestShare = async () => {
    try {
      await bridgeApi.infoShareRequest(targetUserId);
      setShareStatus('pendingSent');
      showToast('已发送互换申请，等待对方同意');
    } catch (e: any) {
      showToast(e?.message || '发送失败，请重试');
    }
  };

  // 同意/拒绝互换(对方申请的状态下)
  const handleShareDecision = async (approve: boolean) => {
    if (!shareId) return;
    try {
      await bridgeApi.infoShareHandle(shareId, approve ? 1 : 2);
      setShareStatus(approve ? 'approved' : 'none');
      showToast(approve ? '已同意互换详细信息' : '已拒绝互换申请');
    } catch (e: any) {
      showToast(e?.message || '操作失败，请重试');
    }
  };

  // 向上滚动加载更早的消息(游标分页,lastId = 当前最小消息ID)
  const loadOlder = async () => {
    if (loadingMore || !hasMore) return;
    const roomId = conversation?.roomId || conversation?.id;
    if (!roomId) return;
    const minId = messages.length ? Math.min(...messages.map(m => m.id)) : undefined;
    setLoadingMore(true);
    try {
      const res = await chatApi.getMessageHistory(roomId, minId, 50);
      const older = (res.data.data.records || []).map((r: any) => ({
        id: r.id,
        fromUserId: r.fromUserId,
        toUserId: r.toUserId,
        content: r.content,
        messageType: r.messageType || 'text',
        isRecalled: r.isRecalled,
        isRead: r.isRead,
        createdAt: r.createdAt,
      }));
      setMessages((prev) => {
        const merged = [...older, ...prev];
        const seen = new Set<number>();
        return merged.filter((m) => (seen.has(m.id) ? false : (seen.add(m.id), true)));
      });
      setHasMore((res.data.data.records?.length || 0) === 50);
    } catch { /* 忽略 */ }
    finally { setLoadingMore(false); }
  };

  // 撤回自己发送的消息
  const handleRecallMessage = async (msg: MessageLike) => {
    try {
      await chatApi.recallMessage(msg.id);
      setMessages((prev) => prev.map((m) => m.id === msg.id ? { ...m, isRecalled: 1, content: '该消息已撤回' } : m));
      showToast('已撤回');
    } catch (e: any) {
      showToast(e?.message || '撤回失败');
    }
  };

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
      <ChatHeader
        title={targetNickname}
        avatar={targetAvatar}
        onBack={() => navigate(-1)}
        extra={
          <ShareStatusTag
            status={shareStatus}
            targetUserId={targetUserId}
            onRequest={handleRequestShare}
            onShowConfirm={() => setShowShareConfirm(true)}
          />
        }
        right={(
          <button
            className="chat-settings-btn"
            title="聊天信息"
            onClick={() => {
              const roomId = conversation?.roomId || conversation?.id;
              navigate(
                `/messages/chat-settings?userId=${targetUserId}` +
                `&name=${encodeURIComponent(targetNickname)}` +
                `&avatar=${encodeURIComponent(targetAvatar)}` +
                `&roomId=${roomId ?? ''}`
              );
            }}
          >
            <AppIcon name="settings" size={20} />
          </button>
        )}
      />

      {/* 消息列表 */}
      <MessageList
        messages={messages}
        currentUserId={user?.id}
        avatarOf={(msg) => msg.fromUserId === user?.id ? (user?.avatar || '') : targetAvatar}
        nicknameOf={(msg) => msg.fromUserId === user?.id ? (user?.nickname || '我') : targetNickname}
        background={resolveChatBg(chatSettings?.background)}
        listRef={msgListRef}
        endRef={messagesEndRef}
        onReportMessage={(msg) => setReportTarget({
          targetType: 'message',
          targetId: msg.id,
          roomId: conversation?.roomId || conversation?.id,
          title: '这条消息',
        })}
        onRecallMessage={handleRecallMessage}
        onLoadMore={loadOlder}
        loadingMore={loadingMore}
        hasMore={hasMore}
      />

      {/* 输入栏 */}
      <ChatInputBar onSend={handleSend} onUploading={setUploading} />

      {/* 对方申请互换信息确认弹窗 */}
      <ConfirmDialog
        visible={showShareConfirm}
        title="互换信息请求"
        message="对方想要互换详细信息，是否同意？"
        confirmText="同意"
        cancelText="拒绝"
        onConfirm={() => { setShowShareConfirm(false); handleShareDecision(true); }}
        onCancel={() => { setShowShareConfirm(false); handleShareDecision(false); }}
      />

      {reportTarget && (
        <ReportDialog
          targetType={reportTarget.targetType}
          targetId={reportTarget.targetId}
          roomId={reportTarget.roomId}
          title={reportTarget.title}
          onClose={() => setReportTarget(null)}
        />
      )}
    </div>
  );
}

