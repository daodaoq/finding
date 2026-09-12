import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import AppIcon from '../../components/AppIcon';
import PageState from '../../components/PageState';
import { formatSessionTime } from '../../utils/format';
import type { Conversation } from '../../types/message';
import type { GroupChat } from '../../types/groupChat';
import './index.css';
import { useProfileClick } from '../../hooks/useProfileClick';

export function previewText(message: string | null | undefined) { if (!message) return '暂无消息'; return message.startsWith('/uploads/') || message.startsWith('http') ? '[图片]' : message; }

interface Props {
  conversations: Conversation[];
  groups: GroupChat[];
  loading: boolean;
  convError: string | null;
  /** 桌面双栏下当前打开的私聊/群聊,用于左栏选中高亮 */
  activeUserId?: number;
  activeGroupId?: number;
  onRetry: () => void;
  onOpenPrivate: (conv: Conversation) => void;
  onOpenGroup: (group: GroupChat) => void;
}

/** 私聊 + 群聊两个会话分区(自 MessagesPage 原样抽出,markup/class 不变)。 */
export default function ConversationSections({ conversations, groups, loading, convError, activeUserId, activeGroupId, onRetry, onOpenPrivate, onOpenGroup }: Props) {
  const profileClick = useProfileClick();
  return <>
    <div className="section-divider">私聊</div>
    <section className="chat-conv-list">
      {loading && <><LoadingSkeleton /><LoadingSkeleton /></>}
      {!loading && conversations.map((conv) => <button key={conv.id} className={`chat-conv-item ${conv.targetUserId === activeUserId ? 'chat-conv-item--active' : ''}`} onClick={() => onOpenPrivate(conv)}>
        <span className="conv-avatar" onClick={profileClick(conv.targetUserId)}>{conv.targetAvatar ? <img src={conv.targetAvatar} alt="" /> : <AppIcon name="user" size={21} />}</span>
        <span className="conv-info"><span className="conv-top"><b>{conv.pinned ? '置顶 · ' : ''}{conv.muted ? '免打扰 · ' : ''}{conv.targetNickname || `用户${conv.targetUserId}`}</b><small>{conv.lastMessageAt ? formatSessionTime(conv.lastMessageAt) : ''}</small></span><span className="conv-bottom"><small>{previewText(conv.lastMessage)}</small>{conv.unreadCount > 0 && <i className="conv-badge">{conv.unreadCount}</i>}</span></span>
      </button>)}
      {!loading && convError && <PageState loading={false} error={convError} onRetry={onRetry} />}
      {!loading && !convError && conversations.length === 0 && <EmptyState message="暂无会话" />}
    </section>
    {groups.length > 0 && <>
      <div className="section-divider">群聊</div>
      <section className="chat-conv-list">
        {groups.map((group) => <button key={group.id} className={`chat-conv-item ${group.id === activeGroupId ? 'chat-conv-item--active' : ''}`} onClick={() => onOpenGroup(group)}>
          <span className="conv-avatar">{group.avatar ? <img src={group.avatar} alt="" /> : <AppIcon name="users" size={21} />}</span>
          <span className="conv-info"><span className="conv-top"><b>{group.name}</b><small>{group.lastMessageAt ? formatSessionTime(group.lastMessageAt) : ''}</small></span><span className="conv-bottom"><small>{previewText(group.lastMessage)}</small></span></span>
        </button>)}
      </section>
    </>}
  </>;
}
