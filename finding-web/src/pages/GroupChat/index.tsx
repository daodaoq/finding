import { useEffect, useState, useRef } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { groupChatApi } from '../../api/groupChat';
import { useAuthStore } from '../../store/authStore';
import { useMessageStore } from '../../store/messageStore';
import { useWebSocket, useWsReconnect } from '../../hooks/useWebSocket';
import { showToast } from '../../components/Toast';
import ChatInputBar from '../../components/ChatInputBar';
import ReportDialog from '../../components/ReportDialog';
import ChatHeader from '../Chat/components/ChatHeader';
import MessageList, { type MessageLike } from '../Chat/components/MessageList';
import AppIcon from '../../components/AppIcon';
import type { GroupMessage, GroupMember } from '../../types/groupChat';
import '../Chat/index.css';

export default function GroupChatPage() {
  const { id } = useParams<{ id: string }>();
  const groupId = Number(id);
  const [searchParams] = useSearchParams();
  const groupName = searchParams.get('name') || '群聊';

  const [messages, setMessages] = useState<GroupMessage[]>([]);
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const pageRef = useRef(1);
  const [reportTarget, setReportTarget] = useState<{
    targetType: string; targetId: number; roomId?: number; title: string;
  } | null>(null);
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const msgListRef = useRef<HTMLDivElement>(null);

  // WebSocket:撤回同步 + 实时群消息(连接由全局单例管理)
  useWebSocket((wsMsg) => {
    if (wsMsg.type === 'message_recalled' && wsMsg.messageId) {
      setMessages((prev) => prev.map((m) =>
        m.id === wsMsg.messageId ? { ...m, isRecalled: 1, content: '该消息已撤回' } : m));
      return;
    }
    if (wsMsg.type === 'group_chat' && wsMsg.conversationId === groupId && wsMsg.messageId) {
      setMessages((prev) => {
        if (prev.some((m) => m.id === wsMsg.messageId)) return prev;
        return [...prev, {
          id: wsMsg.messageId,
          groupId,
          fromUserId: wsMsg.fromUserId,
          fromUserNickname: wsMsg.fromUserNickname || '成员',
          fromUserAvatar: wsMsg.fromUserAvatar || '',
          content: wsMsg.content,
          messageType: wsMsg.messageType || 'text',
          createdAt: new Date().toISOString(),
        } as GroupMessage];
      });
      // 正在看群时收到新消息 → 更新已读位置,角标不误增
      groupChatApi.markRead(groupId).catch(() => {});
      useMessageStore.getState().refreshTotal();
    }
  });

  const scrollToBottom = () => {
    const el = msgListRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  };

  useEffect(() => {
    if (groupId && !isNaN(groupId)) {
      loadMessages();
      // 拉取群成员(供 @ 使用)
      groupChatApi.getGroupDetail(groupId)
        .then((res) => setMembers(res.data.data.members || []))
        .catch(() => {});
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
      pageRef.current = 1;
      setHasMore(true);
      const res = await groupChatApi.getMessageHistory(groupId);
      setMessages(res.data.data.records || []);
    } catch (e) {
      setLoadError(true);
    }
    finally { setLoading(false); }
    // 打开群聊即标记已读并刷新角标
    groupChatApi.markRead(groupId).catch(() => {});
    useMessageStore.getState().refreshTotal();
  };

  // 断线补偿:WS 重连成功后刷新群消息(补拉断线期间缺失)
  useWsReconnect(() => { if (groupId && !isNaN(groupId)) loadMessages(); });

  // 向上滚动加载更早的群消息(分页)
  const loadOlder = async () => {
    if (loadingMore || !hasMore) return;
    setLoadingMore(true);
    try {
      const nextPage = pageRef.current + 1;
      const res = await groupChatApi.getMessageHistory(groupId, nextPage, 50);
      const older = res.data.data.records || [];
      setMessages((prev) => {
        const merged = [...older, ...prev];
        const seen = new Set<number>();
        return merged.filter((m) => (seen.has(m.id) ? false : (seen.add(m.id), true)));
      });
      pageRef.current = nextPage;
      setHasMore(older.length === 50);
    } catch { /* 忽略 */ }
    finally { setLoadingMore(false); }
  };

  useEffect(() => {
    scrollToBottom();
    const t = setTimeout(scrollToBottom, 300);
    return () => clearTimeout(t);
  }, [messages]);

  // 撤回自己发送的群消息
  const handleRecallMessage = async (msg: MessageLike) => {
    try {
      await groupChatApi.recallMessage(groupId, msg.id);
      setMessages((prev) => prev.map((m) => m.id === msg.id ? { ...m, isRecalled: 1, content: '该消息已撤回' } : m));
      showToast('已撤回');
    } catch (e: any) {
      showToast(e?.message || '撤回失败');
    }
  };

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
      const res = await groupChatApi.sendMessage(groupId, content, messageType);
      const real = res.data.data;
      // 用真实消息替换临时消息(避免与 WS 推送重复)
      setMessages((prev) =>
        prev.filter((m) => m.id !== real.id).map((m) => m.id === tempId ? { ...real } : m));
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
            <AppIcon name="info" size={20} />
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
            <AppIcon name="alert" size={18} /> 加载失败，请确认已加入该群聊
          </div>
        ) : undefined}
        emptyNode={!loadError && messages.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 40, color: '#ccc' }}>
            暂无消息，发送第一条消息吧
          </div>
        ) : undefined}
        onReportMessage={(msg) => setReportTarget({
          targetType: 'message',
          targetId: msg.id,
          title: '这条消息',
        })}
        onRecallMessage={handleRecallMessage}
        onLoadMore={loadOlder}
        loadingMore={loadingMore}
        hasMore={hasMore}
      />

      <ChatInputBar
        onSend={handleSend}
        mentionMembers={members.map((m) => ({ userId: m.userId, nickname: m.nickname }))}
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