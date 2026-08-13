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
import PageState from '../../components/PageState';
import { getErrorMessage } from '../../utils/appError';
import { useAuthStore } from '../../store/authStore';
import { formatSessionTime } from '../../utils/format';
import type { Conversation } from '../../types/message';
import type { GroupChat } from '../../types/groupChat';
import './index.css';

export default function MessagesPage() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [groups, setGroups] = useState<GroupChat[]>([]);
  const [hiddenConversations, setHiddenConversations] = useState<Conversation[]>([]);
  const [strangerCount, setStrangerCount] = useState(0);
  const [convError, setConvError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true); const [refreshing, setRefreshing] = useState(false); const [showLogin, setShowLogin] = useState(false);
  const navigate = useNavigate(); const unreadCount = useMessageStore((state) => state.unreadCount); const setUnreadCount = useMessageStore((state) => state.setUnreadCount); const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const loadConversations = async (silent = false) => {
    silent ? setRefreshing(true) : setLoading(true);
    try {
      const response = await chatApi.listConversations();
      setConversations(response.data.data || []);
      setConvError(null);
    } catch (e) {
      // 首次加载失败→错误态+重试;后台刷新失败保留旧数据并提示
      if (conversations.length === 0) setConvError(getErrorMessage(e, '加载会话列表失败'));
      else showToast(getErrorMessage(e, '加载会话列表失败'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };
  const loadGroups = async () => { try { const response = await groupChatApi.listMyGroups(); setGroups(response.data.data || []); } catch { showToast('加载群聊列表失败'); } };
  const loadHiddenConversations = async () => { try { const response = await chatApi.listHiddenConversations(); setHiddenConversations(response.data.data || []); } catch {} };
  const loadStrangerCount = async () => { try { const response = await chatApi.listStrangerMessages(); setStrangerCount((response.data.data || []).length); } catch {} };
  const loadUnreadCount = async () => { try { const response = await messageApi.unreadCount(); setUnreadCount(response.data.data.count); } catch {} };
  useWebSocket(useCallback((message) => {
    if (message.type === 'chat') {
      loadConversations(true);
      // 未读总角标由 MainLayout 的 refreshTotal() 统一从服务端拉取,这里不再本地 +1(避免双重计数与闭包过期)
    }
  }, [loadConversations]));
  useEffect(() => { if (isLoggedIn) { loadConversations(); loadGroups(); loadHiddenConversations(); loadStrangerCount(); loadUnreadCount(); } else setLoading(false); }, [isLoggedIn]);
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
    {isLoggedIn && <><button className="notify-condensed" onClick={() => navigate('/messages/notifications')}><span className="notify-icon-wrap"><AppIcon name="bell" size={20} />{unreadCount > 0 && <span className="notify-badge">{unreadCount}</span>}</span><span className="notify-info"><b>互动通知</b><small>{unreadCount > 0 ? `${unreadCount} 条未读` : '暂无新通知'}</small></span><em>查看</em></button><button className="notify-condensed" onClick={() => navigate('/messages/strangers')}><span className="notify-icon-wrap"><AppIcon name="mail" size={20} />{strangerCount > 0 && <span className="notify-badge">{strangerCount}</span>}</span><span className="notify-info"><b>陌生人消息</b><small>{strangerCount > 0 ? `${strangerCount} 条打招呼待处理` : '暂无陌生人消息'}</small></span><em>查看</em></button><button className="notify-condensed" onClick={() => navigate('/messages/hidden')}><span className="notify-icon-wrap"><AppIcon name="eye" size={20} />{hiddenConversations.length > 0 && <span className="notify-badge">{hiddenConversations.length}</span>}</span><span className="notify-info"><b>隐藏的会话</b><small>{hiddenConversations.length > 0 ? `${hiddenConversations.length} 个会话已隐藏` : '暂无隐藏会话'}</small></span><em>查看</em></button><div className="section-divider">私聊</div><section className="chat-conv-list">{loading && <><LoadingSkeleton /><LoadingSkeleton /></>}{!loading && conversations.map((conv) => <button key={conv.id} className="chat-conv-item" onClick={() => { const name = encodeURIComponent(conv.targetNickname || `用户${conv.targetUserId}`); const avatar = encodeURIComponent(conv.targetAvatar || ''); navigate(`/messages/chat?userId=${conv.targetUserId}&name=${name}&avatar=${avatar}&roomId=${conv.roomId || conv.id}`); }}><span className="conv-avatar">{conv.targetAvatar ? <img src={conv.targetAvatar} alt="" /> : <AppIcon name="user" size={21} />}</span><span className="conv-info"><span className="conv-top"><b>{conv.pinned ? '置顶 · ' : ''}{conv.muted ? '免打扰 · ' : ''}{conv.targetNickname || `用户${conv.targetUserId}`}</b><small>{conv.lastMessageAt ? formatSessionTime(conv.lastMessageAt) : ''}</small></span><span className="conv-bottom"><small>{previewText(conv.lastMessage)}</small>{conv.unreadCount > 0 && <i className="conv-badge">{conv.unreadCount}</i>}</span></span></button>)}{!loading && convError && <PageState loading={false} error={convError} onRetry={() => loadConversations()} />}
{!loading && !convError && conversations.length === 0 && <EmptyState message="暂无会话" />}</section>{groups.length > 0 && <><div className="section-divider">群聊</div><section className="chat-conv-list">{groups.map((group) => <button key={group.id} className="chat-conv-item" onClick={() => navigate(`/messages/group-chat/${group.id}?name=${encodeURIComponent(group.name)}`)}><span className="conv-avatar">{group.avatar ? <img src={group.avatar} alt="" /> : <AppIcon name="users" size={21} />}</span><span className="conv-info"><span className="conv-top"><b>{group.name}</b><small>{group.lastMessageAt ? formatSessionTime(group.lastMessageAt) : ''}</small></span><span className="conv-bottom"><small>{previewText(group.lastMessage)}</small></span></span></button>)}</section></>}
      </>}
    <LoginModal visible={showLogin} onClose={() => setShowLogin(false)} onSuccess={() => { setShowLogin(false); window.location.reload(); }} /></div>;
}
function previewText(message: string | null | undefined) { if (!message) return '暂无消息'; return message.startsWith('/uploads/') || message.startsWith('http') ? '[图片]' : message; }
