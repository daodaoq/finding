import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AppIcon from '../../components/AppIcon';
import LoginModal from '../../components/LoginModal';
import { useMessageStore } from '../../store/messageStore';
import type { Conversation } from '../../types/message';
import type { GroupChat } from '../../types/groupChat';
import { useConversations } from './useConversations';
import ConversationSections from './ConversationSections';

/**
 * 桌面双栏布局的左侧会话栏:紧凑版消息中心(标题 + 通知入口 + 会话分区)。
 * 数据层与移动端 MessagesPage 相同;桌面下整页列表不再渲染,仅此栏存在一份。
 */
export default function ConversationPane() {
  const {
    conversations, groups, hiddenConversations, strangerCount,
    convError, loading, refreshing, isLoggedIn, loadConversations, refresh,
  } = useConversations();
  const navigate = useNavigate();
  const location = useLocation();
  const [showLogin, setShowLogin] = useState(false);
  const unreadCount = useMessageStore((state) => state.unreadCount);

  // 从当前路由解析选中的私聊/群聊,驱动左栏高亮
  const params = new URLSearchParams(location.search);
  const userIdParam = Number(params.get('userId'));
  const activeUserId = location.pathname === '/messages/chat' && Number.isFinite(userIdParam) && userIdParam > 0 ? userIdParam : undefined;
  const groupMatch = location.pathname.match(/^\/messages\/group-chat\/(\d+)/);
  const activeGroupId = groupMatch ? Number(groupMatch[1]) : undefined;

  const openPrivate = (conv: Conversation) => {
    const name = encodeURIComponent(conv.targetNickname || `用户${conv.targetUserId}`);
    const avatar = encodeURIComponent(conv.targetAvatar || '');
    navigate(`/messages/chat?userId=${conv.targetUserId}&name=${name}&avatar=${avatar}&roomId=${conv.roomId || conv.id}`);
  };
  const openGroup = (group: GroupChat) => navigate(`/messages/group-chat/${group.id}?name=${encodeURIComponent(group.name)}`);

  return <div className="conv-pane-inner">
    <header className="msg-header"><h2 className="msg-header-title">互动消息</h2><div className="msg-header-actions"><button className="header-create-group-btn" onClick={() => navigate('/messages/create-group')}>建群</button><button className="header-action-btn" onClick={refresh} aria-label="刷新"><AppIcon name="refresh" size={19} className={refreshing ? 'is-spinning' : ''} /></button></div></header>
    {!isLoggedIn && <button className="msg-login-prompt" onClick={() => setShowLogin(true)}><AppIcon name="user" size={22} /><span><b>登录后查看消息</b><small>登录后可查看互动通知和私聊消息</small></span><em>登录</em></button>}
    {isLoggedIn && <>
      <button className="notify-condensed" onClick={() => navigate('/messages/notifications')}><span className="notify-icon-wrap"><AppIcon name="bell" size={20} />{unreadCount > 0 && <span className="notify-badge">{unreadCount}</span>}</span><span className="notify-info"><b>互动通知</b><small>{unreadCount > 0 ? `${unreadCount} 条未读` : '暂无新通知'}</small></span><em>查看</em></button>
      <button className="notify-condensed" onClick={() => navigate('/messages/strangers')}><span className="notify-icon-wrap"><AppIcon name="mail" size={20} />{strangerCount > 0 && <span className="notify-badge">{strangerCount}</span>}</span><span className="notify-info"><b>陌生人消息</b><small>{strangerCount > 0 ? `${strangerCount} 条打招呼待处理` : '暂无陌生人消息'}</small></span><em>查看</em></button>
      <button className="notify-condensed" onClick={() => navigate('/messages/hidden')}><span className="notify-icon-wrap"><AppIcon name="eye" size={20} />{hiddenConversations.length > 0 && <span className="notify-badge">{hiddenConversations.length}</span>}</span><span className="notify-info"><b>隐藏的会话</b><small>{hiddenConversations.length > 0 ? `${hiddenConversations.length} 个会话已隐藏` : '暂无隐藏会话'}</small></span><em>查看</em></button>
      <ConversationSections
        conversations={conversations}
        groups={groups}
        loading={loading}
        convError={convError}
        activeUserId={activeUserId}
        activeGroupId={activeGroupId}
        onRetry={() => loadConversations()}
        onOpenPrivate={openPrivate}
        onOpenGroup={openGroup}
      />
    </>}
    <LoginModal visible={showLogin} onClose={() => setShowLogin(false)} onSuccess={() => { setShowLogin(false); window.location.reload(); }} />
  </div>;
}
