import type { CSSProperties, ReactNode, Ref } from 'react';
import ChatBubble from '../../../components/ChatBubble';
import { formatChatTime } from '../../../utils/format';
import './MessageList.css';

/** 私聊/群聊消息在列表中共用的最小结构 */
export interface MessageLike {
  id: number;
  fromUserId: number;
  content: string;
  messageType: string;
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
}

/** 聊天消息列表：10 分钟间隔时间分隔线 + 消息气泡。私聊 / 群聊共用。 */
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
}: Props) {
  return (
    <div className="chat-messages" style={background} ref={listRef}>
      {errorNode}
      {emptyNode}
      {messages.map((msg, i) => {
        const prevMsg = i > 0 ? messages[i - 1] : null;
        const showTimeSep = !prevMsg ||
          (new Date(msg.createdAt).getTime() - new Date(prevMsg.createdAt).getTime()) > 10 * 60 * 1000;
        return (
          <div key={msg.id}>
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
            />
          </div>
        );
      })}
      <div ref={endRef || undefined} />
    </div>
  );
}