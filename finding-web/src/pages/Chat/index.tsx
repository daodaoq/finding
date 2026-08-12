import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { chatSocket } from '../../ws/chatSocket';
import ChatInputBar from '../../components/ChatInputBar';
import AppIcon from '../../components/AppIcon';
import ConfirmDialog from '../../components/ConfirmDialog';
import ReportDialog from '../../components/ReportDialog';
import ChatHeader from './components/ChatHeader';
import ShareStatusTag from './components/ShareStatusTag';
import MessageList from './components/MessageList';
import { useChatSession } from './hooks/useChatSession';
import { useChatSocket } from './hooks/useChatSocket';
import { useChatActions } from './hooks/useChatActions';
import { resolveChatBg } from '../../utils/chatBackgrounds';
import './index.css';

export default function ChatDetailPage() {
  const [searchParams] = useSearchParams();
  const targetUserId = Number(searchParams.get('userId'));
  // 昵称/头像仅作加载时的兜底展示,进入会话后以服务端会话接口返回为准(URL 参数不可信)
  const urlNickname = searchParams.get('name') || '聊天';
  const urlAvatar = searchParams.get('avatar') || '';

  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const msgListRef = useRef<HTMLDivElement>(null);

  // ── 三层逻辑拆分为独立 hooks ──
  const session = useChatSession({
    targetUserId,
    user,
    // 初始化失败(无会话/无权限)已提示 → 回退上一页
    onInitError: () => navigate(-1),
  });

  const socket = useChatSocket({
    targetUserId,
    user,
    setMessages: session.setMessages,
    refreshFromServer: session.refreshFromServer,
  });

  const actions = useChatActions({
    targetUserId,
    user,
    conversation: session.conversation,
    setMessages: session.setMessages,
  });

  // 对方昵称/头像:服务端会话返回为准,URL 参数仅兜底
  const targetNickname = session.conversation?.targetNickname || urlNickname;
  const targetAvatar = session.conversation?.targetAvatar || urlAvatar;

  // ── 纯 UI 状态 ──
  const [, setUploading] = useState(false);
  const [showShareConfirm, setShowShareConfirm] = useState(false);
  const [reportTarget, setReportTarget] = useState<{
    targetType: string; targetId: number; roomId?: number; title: string;
  } | null>(null);

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
  }, [session.messages]);

  if (session.loading) {
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
        subtitle={socket.isTyping ? '对方正在输入…' : undefined}
        avatar={targetAvatar}
        onBack={() => navigate(-1)}
        extra={
          <ShareStatusTag
            status={actions.shareStatus}
            targetUserId={targetUserId}
            onRequest={actions.handleRequestShare}
            onShowConfirm={() => setShowShareConfirm(true)}
          />
        }
        right={(
          <button
            className="chat-settings-btn"
            title="聊天信息"
            onClick={() => {
              const roomId = session.conversation?.roomId || session.conversation?.id;
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
        messages={session.messages}
        currentUserId={user?.id}
        avatarOf={(msg) => msg.fromUserId === user?.id ? (user?.avatar || '') : targetAvatar}
        nicknameOf={(msg) => msg.fromUserId === user?.id ? (user?.nickname || '我') : targetNickname}
        background={resolveChatBg(session.chatSettings?.background)}
        listRef={msgListRef}
        endRef={messagesEndRef}
        onReportMessage={(msg) => setReportTarget({
          targetType: 'message',
          targetId: msg.id,
          roomId: session.conversation?.roomId || session.conversation?.id,
          title: '这条消息',
        })}
        onRecallMessage={actions.recallMessage}
        onRetryMessage={actions.retryMessage}
        onReplyMessage={(msg) => actions.setReplyTo({
          id: msg.id,
          content: msg.messageType === 'image' ? '[图片]' : msg.messageType === 'video' ? '[视频]' : msg.content,
          nickname: msg.fromUserId === user?.id ? '我' : targetNickname,
        })}
        onLoadMore={session.loadOlder}
        loadingMore={session.loadingMore}
        hasMore={session.hasMore}
      />

      {/* 输入栏 */}
      <ChatInputBar
        onSend={actions.sendMessage}
        onUploading={setUploading}
        replyTo={actions.replyTo}
        onCancelReply={() => actions.setReplyTo(null)}
        onTyping={() => {
          const roomId = session.conversation?.roomId || session.conversation?.id;
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
        onConfirm={() => { setShowShareConfirm(false); actions.handleShareDecision(true); }}
        onCancel={() => { setShowShareConfirm(false); actions.handleShareDecision(false); }}
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
