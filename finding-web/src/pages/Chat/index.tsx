import { useEffect, useState, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { chatApi } from '../../api/chat';
import { bridgeApi } from '../../api/bridge';
import { useWebSocket, useWsReconnect } from '../../hooks/useWebSocket';
import { chatSocket } from '../../ws/chatSocket';
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
  /** 客户端幂等 ID(弱网重试复用,服务端据此去重) */
  clientMessageId?: string;
  /** 发送状态(仅自己刚发送/失败的消息) */
  sendState?: 'sending' | 'sent' | 'failed';
}

export default function ChatDetailPage() {
  const [searchParams] = useSearchParams();
  const targetUserId = Number(searchParams.get('userId'));
  // 昵称/头像仅作加载时的兜底展示,进入会话后以服务端会话接口返回为准(URL 参数不可信)
  const [targetNickname, setTargetNickname] = useState(searchParams.get('name') || '聊天');
  const [targetAvatar, setTargetAvatar] = useState(searchParams.get('avatar') || '');

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
  // 回复/引用目标(输入栏显示引用条)
  const [replyTo, setReplyTo] = useState<{ id: number; content: string; nickname: string } | null>(null);
  // 对方正在输入…
  const [isTyping, setIsTyping] = useState(false);
  const typingTimerRef = useRef<ReturnType<typeof setTimeout>>();
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const shareVersion = useInfoShareStore((s) => s.version);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const msgListRef = useRef<HTMLDivElement>(null);

  // WebSocket 实时消息处理(连接由全局单例管理)
  useWebSocket((wsMsg) => {
    if (wsMsg.type === 'message_recalled' && wsMsg.messageId) {
      setMessages((prev) => prev.map((m) =>
        m.id === wsMsg.messageId ? { ...m, isRecalled: 1, content: '该消息已撤回' } : m));
      return;
    }
    // 对方正在输入…(3 秒后自动消失)
    if (wsMsg.type === 'typing' && wsMsg.fromUserId === targetUserId) {
      setIsTyping(true);
      if (typingTimerRef.current) clearTimeout(typingTimerRef.current);
      typingTimerRef.current = setTimeout(() => setIsTyping(false), 3000);
      return;
    }
    // 接收对方消息 + 自己其他设备发送的消息(多端同步);按真实 messageId 去重
    const isRelevant = wsMsg.type === 'chat' && wsMsg.messageId
      && (wsMsg.fromUserId === targetUserId || wsMsg.fromUserId === user?.id);
    if (isRelevant) {
      setMessages((prev) => {
        if (prev.some((m) => m.id === wsMsg.messageId)) return prev;
        return [...prev, {
          id: wsMsg.messageId,
          fromUserId: wsMsg.fromUserId,
          toUserId: wsMsg.toUserId,
          content: wsMsg.content,
          messageType: wsMsg.messageType || 'text',
          isRead: 0,
          parentMessageId: wsMsg.parentMessageId,
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

  /** 后端消息记录 → 本地消息结构 */
  const toMsg = (r: any): ChatMessage => ({
    id: r.id,
    fromUserId: r.fromUserId,
    toUserId: r.toUserId,
    content: r.content,
    messageType: r.messageType || 'text',
    isRecalled: r.isRecalled,
    isRead: r.isRead,
    parentMessageId: r.parentMessageId,
    createdAt: r.createdAt,
  });

  const initConversation = async () => {
    try {
      setLoading(true);
      // 获取已有会话(不存在则服务端拒绝——新会话只能由『相亲桥』聊天申请批准建立)
      const convRes = await chatApi.getOrCreateConversation(targetUserId);
      const conv = convRes.data.data;
      setConversation(conv);
      // 对方资料以服务端会话返回为准,覆盖 URL 携带的昵称/头像
      if (conv.targetNickname) setTargetNickname(conv.targetNickname);
      if (conv.targetAvatar) setTargetAvatar(conv.targetAvatar);
      // 使用 roomId 加载消息历史
      const roomId = conv.roomId || conv.id;
      const msgRes = await chatApi.getMessageHistory(roomId);
      const records = (msgRes.data.data.records || []).map(toMsg);
      setMessages(records);
      // 进入会话已标记已读 → 刷新汇总角标
      useMessageStore.getState().refreshTotal();
    } catch (e: any) {
      console.error('初始化会话失败', e);
      showToast(e?.message || '还没有会话，请先通过『相亲桥』发起聊天申请');
      // 会话不存在或无权限:回到上一页并提示原因
      navigate(-1);
    } finally {
      setLoading(false);
    }
  };

  /** 断线补偿/回前台:拉取最新 50 条,按 id 合并去重(补回断线期间缺失的消息) */
  const refreshFromServer = async () => {
    const roomId = conversation?.roomId || conversation?.id;
    if (!roomId || !user) return;
    try {
      const res = await chatApi.getMessageHistory(roomId);
      const fresh = (res.data.data.records || []).map(toMsg);
      setMessages((prev) => {
        const merged = [...prev];
        for (const m of fresh) {
          if (!merged.some((x) => x.id === m.id)) merged.push(m);
        }
        return merged.sort((a, b) => a.id - b.id);
      });
      useMessageStore.getState().refreshTotal();
    } catch { /* 忽略 */ }
  };

  // 断线补偿:WS 重连成功后补拉当前会话
  useWsReconnect(refreshFromServer);
  // 应用切回前台时刷新
  useEffect(() => {
    const onVisible = () => { if (document.visibilityState === 'visible') refreshFromServer(); };
    document.addEventListener('visibilitychange', onVisible);
    return () => document.removeEventListener('visibilitychange', onVisible);
  });

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
      const older = (res.data.data.records || []).map(toMsg);
      setMessages((prev) => {
        const merged = [...older, ...prev];
        const seen = new Set<number>();
        return merged.filter((m) => (seen.has(m.id) ? false : (seen.add(m.id), true)));
      });
      // 游标分页:后端"多查一条"判定 hasMore,不再按 50 条整页猜测
      setHasMore(!!res.data.data.hasMore);
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

  // 发送消息(以 roomId 指定会话;成功后用真实消息回执替换临时消息)
  const handleSend = async (content: string, messageType = 'text') => {
    if (!user || !conversation) return;
    const roomId = conversation.roomId || conversation.id;
    if (!roomId) return;
    const tempId = Date.now();
    const newMsg: ChatMessage = {
      id: tempId, // 临时 ID
      fromUserId: user.id,
      toUserId: targetUserId,
      content,
      messageType,
      isRead: 0,
      createdAt: new Date().toISOString(),
      clientMessageId: `${user.id}_${tempId}`,
      parentMessageId: replyTo?.id,
      sendState: 'sending',
    };
    setMessages((prev) => [...prev, newMsg]);

    try {
      const res = await chatApi.sendMessage({
        roomId, content, messageType, clientMessageId: newMsg.clientMessageId, replyToMessageId: replyTo?.id,
      });
      const real = res.data.data;
      // 用真实消息替换临时消息(避免与 WS 推送重复;同一 clientMessageId 重试返回同一条)
      setMessages((prev) =>
        prev.filter((m) => m.id !== real.id).map((m) =>
          m.id === tempId
            ? { ...m, id: real.id, createdAt: real.createdAt, isRead: real.isRead ?? 0, sendState: 'sent' as const }
            : m));
    } catch (e: any) {
      // 失败不删除:标记失败,可点击重试
      setMessages((prev) => prev.map((m) => m.id === tempId ? { ...m, sendState: 'failed' as const } : m));
      showToast(e?.message || '发送失败，请稍后重试');
    }
    // 无论成败都清除引用条(失败消息保留 parentMessageId,重试时复用)
    setReplyTo(null);
  };

  // 重试失败的发送(复用同一 clientMessageId,服务端幂等去重)
  const retryMessage = async (msg: MessageLike) => {
    const failed = msg as ChatMessage;
    if (!conversation) return;
    const roomId = conversation.roomId || conversation.id;
    if (!roomId) return;
    setMessages((prev) => prev.map((m) => m.id === failed.id ? { ...m, sendState: 'sending' as const } : m));
    try {
      const res = await chatApi.sendMessage({
        roomId, content: failed.content, messageType: failed.messageType,
        clientMessageId: failed.clientMessageId, replyToMessageId: failed.parentMessageId,
      });
      const real = res.data.data;
      setMessages((prev) =>
        prev.filter((m) => m.id !== real.id).map((m) =>
          m.id === failed.id
            ? { ...m, id: real.id, createdAt: real.createdAt, isRead: real.isRead ?? 0, sendState: 'sent' as const }
            : m));
    } catch (e: any) {
      setMessages((prev) => prev.map((m) => m.id === failed.id ? { ...m, sendState: 'failed' as const } : m));
      showToast(e?.message || '重试失败，请稍后再试');
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
        subtitle={isTyping ? '对方正在输入…' : undefined}
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
        onRetryMessage={retryMessage}
        onReplyMessage={(msg) => setReplyTo({
          id: msg.id,
          content: msg.messageType === 'image' ? '[图片]' : msg.content,
          nickname: msg.fromUserId === user?.id ? '我' : targetNickname,
        })}
        onLoadMore={loadOlder}
        loadingMore={loadingMore}
        hasMore={hasMore}
      />

      {/* 输入栏 */}
      <ChatInputBar
        onSend={handleSend}
        onUploading={setUploading}
        replyTo={replyTo}
        onCancelReply={() => setReplyTo(null)}
        onTyping={() => {
          const roomId = conversation?.roomId || conversation?.id;
          if (roomId && user) chatSocket.sendTyping(roomId, targetUserId);
        }}
      />

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

