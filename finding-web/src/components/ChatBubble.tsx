import './ChatBubble.css';

interface ChatMessage {
  id: number;
  fromUserId: number;
  content: string;
  messageType: string;
  createdAt: string;
}

interface Props {
  message: ChatMessage;
  isMine: boolean;
  avatar?: string;
  nickname?: string;
}

export default function ChatBubble({ message, isMine, avatar, nickname }: Props) {
  return (
    <div className={`chat-bubble-row ${isMine ? 'mine' : 'other'}`}>
      {/* 对方的头像在最左，我的头像在最右 */}
      <div className="chat-avatar">
        {avatar ? <img src={avatar} alt="" /> : <span>👤</span>}
      </div>
      <div className="chat-bubble-wrapper">
        {!isMine && <span className="chat-sender">{nickname}</span>}
        <div className={`chat-bubble ${isMine ? 'bubble-mine' : 'bubble-other'}`}>
          {message.messageType === 'image' ? (
            <img src={message.content} alt="" className="chat-image" />
          ) : (
            <span>{message.content}</span>
          )}
        </div>
        <span className="chat-time">{formatTime(message.createdAt)}</span>
      </div>
    </div>
  );
}

function formatTime(dateStr: string): string {
  const d = new Date(dateStr);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}
