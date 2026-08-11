import type { CSSProperties, ReactNode, Ref } from 'react';
import ChatBubble from '../../../components/ChatBubble';
import { formatChatTime, formatDateHeader } from '../../../utils/format';
import './MessageList.css';

/** 私聊/群聊消息在列表中共用的最小结构 */
export interface MessageLike {
  id: number;
  fromUserId: number;
  content: string;
  messageType: string;
  isRecalled?: number;
  /** 回复/引用:被回复消息 ID(前端在本地消息中查原文渲染) */
  parentMessageId?: number;
  createdAt: string;
}

interface Props {
  messages: MessageLike[];
  currentUserId?: number;
  avatarOf: (msg: MessageLike) => string;
  nicknameOf: (msg: MessageLike) => string;
  /** 背景样式（聊天背景色/图） */
  background?: CSSProperties;
  listRef?: Ref<HTMLDivElement>;
  /** 滚动容器末尾的锚点元素 */
  endRef?: Ref<HTMLDivElement>;
  /** 加载失败提示（显示在消息列表顶部） */
  errorNode?: ReactNode;
  /** 空列表提示 */
  emptyNode?: ReactNode;
  /** 长按消息投诉 */
  onReportMessage?: (msg: MessageLike) => void;
  /** 长按消息撤回(仅自己的消息) */
  onRecallMessage?: (msg: MessageLike) => void;
  /** 点击失败消息重试发送 */
  onRetryMessage?: (msg: MessageLike) => void;
  /** 长按消息「回复」 */
  onReplyMessage?: (msg: MessageLike) => void;
  /** 滚动到顶部时加载更早消息 */
  onLoadMore?: () => void;
  loadingMore?: boolean;
  hasMore?: boolean;
}

function isSameDay(a: string, b: string): boolean {
  const da = new Date(a);
  const db = new Date(b);
  return da.getFullYear() === db.getFullYear()
    && da.getMonth() === db.getMonth()
    && da.getDate() === db.getDate();
}

/** 聊天消息列表：日期分组头 + 10 分钟时间分隔线 + 消息气泡(含回复引用)。私聊 / 群聊共用。 */
export default function MessageList({
  messages,
  currentUserId,
  avatarOf,
  nicknameOf,
  background,
  listRef,
  endRef,
  errorNode,
  emptyNode,
  onReportMessage,
  onRecallMessage,
  onRetryMessage,
  onReplyMessage,
  onLoadMore,
  loadingMore,
  hasMore,
}: Props) {
  return (
    <div
      className="chat-messages"
      style={background}
      ref={listRef}
      onScroll={(e) => {
        const el = e.currentTarget;
        if (hasMore && !loadingMore && el.scrollTop < 30) onLoadMore?.();
      }}
    >
      {errorNode}
      {emptyNode}
      {messages.map((msg, i) => {
        const prevMsg = i > 0 ? messages[i - 1] : null;
        const showDateHeader = !prevMsg || !isSameDay(msg.createdAt, prevMsg.createdAt);
        const showTimeSep = !prevMsg ||
          (new Date(msg.createdAt).getTime() - new Date(prevMsg.createdAt).getTime()) > 10 * 60 * 1000;
        // 回复/引用:在本地消息中查找被回复消息(不在则只显示占位)
        const replyTo = msg.parentMessageId
          ? messages.find((m) => m.id === msg.parentMessageId) || null
          : null;
        return (
          <div key={msg.id}>
            {showDateHeader && (
              <div className="chat-date-sep">
                <span>{formatDateHeader(msg.createdAt)}</span>
              </div>
            )}
            {showTimeSep && (
              <div className="chat-time-sep">
                <span>{formatChatTime(msg.createdAt)}</span>
              </div>
            )}
            <ChatBubble
              message={msg}
              isMine={msg.fromUserId === currentUserId}
              avatar={avatarOf(msg)}
              nickname={nicknameOf(msg)}
              replyTo={replyTo}
              replyToName={replyTo ? nicknameOf(replyTo) : undefined}
              onReport={onReportMessage}
              onRecall={onRecallMessage}
              onRetry={onRetryMessage}
              onReply={onReplyMessage}
            />
          </div>
        );
      })}
      <div ref={endRef || undefined} />
    </div>
  );
}
