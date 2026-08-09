import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { formatClockTime } from '../utils/format';
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
  /** 长按消息触发投诉 */
  onReport?: (message: ChatMessage) => void;
}

export default function ChatBubble({ message, isMine, avatar, nickname, onReport }: Props) {
  const [preview, setPreview] = useState<string | null>(null);
  const navigate = useNavigate();
  const longPressTimer = useRef<ReturnType<typeof setTimeout>>();

  const handleAvatarClick = () => {
    if (message.fromUserId) {
      navigate(`/user/${message.fromUserId}`);
    }
  };

  // 长按 600ms 触发投诉
  const startPress = () => {
    longPressTimer.current = setTimeout(() => {
      onReport?.(message);
    }, 600);
  };
  const clearPress = () => {
    if (longPressTimer.current) clearTimeout(longPressTimer.current);
  };

  return (
    <>
      <div
        className={`chat-bubble-row ${isMine ? 'mine' : 'other'}`}
        onTouchStart={startPress}
        onTouchEnd={clearPress}
        onTouchMove={clearPress}
        onContextMenu={(e) => { e.preventDefault(); onReport?.(message); }}
      >
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
          <span className="chat-time">{formatClockTime(message.createdAt)}</span>
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
