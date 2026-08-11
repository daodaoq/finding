import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { messageApi } from '../../api/message';
import { chatApi } from '../../api/chat';
import { groupChatApi } from '../../api/groupChat';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import LoginModal from '../../components/LoginModal';
import AppIcon from '../../components/AppIcon';
import { showToast } from '../../components/Toast';
import { useMessageStore } from '../../store/messageStore';
import { useWebSocket, useWsReconnect } from '../../hooks/useWebSocket';
import { useAuthStore } from '../../store/authStore';
import { formatSessionTime } from '../../utils/format';
import type { Conversation } from '../../types/message';
import type { GroupChat } from '../../types/groupChat';
import './index.css';

export default function MessagesPage() {
  const [conversations, setConversations] = useState<Conversation[]>([]); const [groups, setGroups] = useState<GroupChat[]>([]); const [loading, setLoading] = useState(true); const [refreshing, setRefreshing] = useState(false); const [showLogin, setShowLogin] = useState(false);
  const navigate = useNavigate(); const unreadCount = useMessageStore((state) => state.unreadCount); const setUnreadCount = useMessageStore((state) => state.setUnreadCount); const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const loadConversations = async (silent = false) => { silent ? setRefreshing(true) : setLoading(true); try { const response = await chatApi.listConversations(); setConversations(response.data.data || []); } catch { showToast('加载会话列表失败'); } finally { setLoading(false); setRefreshing(false); } };
  const loadGroups = async () => { try { const response = await groupChatApi.listMyGroups(); setGroups(response.data.data || []); } catch { showToast('加载群聊列表失败'); } };
  const loadUnreadCount = async () => { try { const response = await messageApi.unreadCount(); setUnreadCount(response.data.data.count); } catch {} };
  const myId = useAuthStore((s) => s.user?.id);
  useWebSocket(useCallback((message) => {
    if (message.type === 'chat') {
      loadConversations(true);
      // 自己发送的消息(其他设备同步)不计入未读
      if (message.fromUserId !== myId) setUnreadCount(unreadCount + 1);
    }
  }, [unreadCount, myId]));
  useEffect(() => { if (isLoggedIn) { loadConversations(); loadGroups(); loadUnreadCount(); } else setLoading(false); }, [isLoggedIn]);
  const refresh = async () => { await loadConversations(true); await loadUnreadCount(); };
  // 断线补偿:WS 重连成功后刷新会话列表与未读
  const refreshRef = useRef(refresh); refreshRef.current = refresh;
  useWsReconnect(() => refreshRef.current());
  // 回前台时刷新会话列表(补拉断线/后台期间的消息)
  useEffect(() => {
    const onFocus = () => { if (isLoggedIn) refreshRef.current(); };
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, [isLoggedIn]);
  return <div className="messages-page"><header className="msg-header"><h2 className="msg-header-title">互动消息</h2><div className="msg-header-actions"><button className="header-create-group-btn" onClick={() => navigate('/messages/create-group')}>建群</button><button className="header-action-btn" onClick={refresh} aria-label="刷新"><AppIcon name="refresh" size={19} className={refreshing ? 'is-spinning' : ''} /></button></div></header>
    {!isLoggedIn && <button className="msg-login-prompt" onClick={() => setShowLogin(true)}><AppIcon name="user" size={22} /><span><b>登录后查看消息</b><small>登录后可查看互动通知和私聊消息</small></span><em>登录</em></button>}
    {isLoggedIn && <><button className="notify-condensed" onClick={() => navigate('/messages/notifications')}><span className="notify-icon-wrap"><AppIcon name="bell" size={20} />{unreadCount > 0 && <span className="notify-badge">{unreadCount}</span>}</span><span className="notify-info"><b>互动通知</b><small>{unreadCount > 0 ? `${unreadCount} 条未读` : '暂无新通知'}</small></span><em>查看</em></button><div className="section-divider">私聊</div><section className="chat-conv-list">{loading && <><LoadingSkeleton /><LoadingSkeleton /></>}{!loading && conversations.map((conv) => <button key={conv.id} className="chat-conv-item" onClick={() => { const name = encodeURIComponent(conv.targetNickname || `用户${conv.targetUserId}`); const avatar = encodeURIComponent(conv.targetAvatar || ''); navigate(`/messages/chat?userId=${conv.targetUserId}&name=${name}&avatar=${avatar}&roomId=${conv.roomId || conv.id}`); }}><span className="conv-avatar">{conv.targetAvatar ? <img src={conv.targetAvatar} alt="" /> : <AppIcon name="user" size={21} />}</span><span className="conv-info"><span className="conv-top"><b>{conv.pinned ? '置顶 · ' : ''}{conv.muted ? '免打扰 · ' : ''}{conv.targetNickname || `用户${conv.targetUserId}`}</b><small>{conv.lastMessageAt ? formatSessionTime(conv.lastMessageAt) : ''}</small></span><span className="conv-bottom"><small>{previewText(conv.lastMessage)}</small>{conv.unreadCount > 0 && <i className="conv-badge">{conv.unreadCount}</i>}</span></span></button>)}{!loading && conversations.length === 0 && <EmptyState message="暂无会话" />}</section>{groups.length > 0 && <><div className="section-divider">群聊</div><section className="chat-conv-list">{groups.map((group) => <button key={group.id} className="chat-conv-item" onClick={() => navigate(`/messages/group-chat/${group.id}?name=${encodeURIComponent(group.name)}`)}><span className="conv-avatar">{group.avatar ? <img src={group.avatar} alt="" /> : <AppIcon name="users" size={21} />}</span><span className="conv-info"><span className="conv-top"><b>{group.name}</b><small>{group.lastMessageAt ? formatSessionTime(group.lastMessageAt) : ''}</small></span><span className="conv-bottom"><small>{previewText(group.lastMessage)}</small></span></span></button>)}</section></>}</>}
    <LoginModal visible={showLogin} onClose={() => setShowLogin(false)} onSuccess={() => { setShowLogin(false); window.location.reload(); }} /></div>;
}
function previewText(message: string | null | undefined) { if (!message) return '暂无消息'; return message.startsWith('/uploads/') || message.startsWith('http') ? '[图片]' : message; }
