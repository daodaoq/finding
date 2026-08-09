import type { Message } from '../types/message';
import { formatSessionTime } from '../utils/format';
import './MessageItem.css';

interface Props {
  message: Message;
  onClick: (msg: Message) => void;
}

export default function MessageItem({ message, onClick }: Props) {
  return (
    <div
      className={`message-item ${!message.isRead ? 'unread' : ''}`}
      onClick={() => onClick(message)}
    >
      <div className="msg-avatar">
        {message.fromUserAvatar ? (
          <img src={message.fromUserAvatar} alt="" />
        ) : (
          <span>📢</span>
        )}
      </div>
      <div className="msg-content">
        <div className="msg-title">
          <span className="msg-name">{message.fromUserNickname || '系统'}</span>
          <span className="msg-type">{message.typeDesc || message.type}</span>
        </div>
        <div className="msg-text">{message.content}</div>
      </div>
      <div className="msg-right">
        <span className="msg-time">{formatSessionTime(message.createdAt)}</span>
        {!message.isRead && <span className="msg-badge" />}
      </div>
    </div>
  );
}
