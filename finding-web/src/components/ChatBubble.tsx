import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
  const [preview, setPreview] = useState<string | null>(null);
  const navigate = useNavigate();

  const handleAvatarClick = () => {
    if (message.fromUserId) {
      navigate(`/user/${message.fromUserId}`);
    }
  };

  return (
    <>
      <div className={`chat-bubble-row ${isMine ? 'mine' : 'other'}`}>
        <div className="chat-avatar" onClick={handleAvatarClick} style={{ cursor: 'pointer' }}>
          {avatar ? <img src={avatar} alt="" /> : <span>👤</span>}
        </div>
        <div className="chat-bubble-wrapper">
          {!isMine && <span className="chat-sender">{nickname}</span>}
          <div className={`chat-bubble ${isMine ? 'bubble-mine' : 'bubble-other'}`}>
            {message.messageType === 'image' ? (
              <img
                src={message.content}
                alt=""
                className="chat-image"
                onClick={() => setPreview(message.content)}
              />
            ) : (
              <span>{message.content}</span>
            )}
          </div>
          <span className="chat-time">{formatTime(message.createdAt)}</span>
        </div>
      </div>

      {/* 图片预览遮罩 */}
      {preview && (
        <div className="image-preview-overlay" onClick={() => setPreview(null)}>
          <img src={preview} alt="" className="image-preview-img" onClick={(e) => e.stopPropagation()} />
          <button className="image-preview-close" onClick={() => setPreview(null)}>✕</button>
        </div>
      )}
    </>
  );
}

function formatTime(dateStr: string): string {
  const d = new Date(dateStr);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}
