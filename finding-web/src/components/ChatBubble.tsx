import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { formatClockTime } from '../utils/format';
import AppIcon from './AppIcon';
import './ChatBubble.css';

interface ChatMessage {
  id: number;
  fromUserId: number;
  content: string;
  messageType: string;
  isRecalled?: number;
  createdAt: string;
}

interface Props {
  message: ChatMessage;
  isMine: boolean;
  avatar?: string;
  nickname?: string;
  /** 长按消息触发举报 */
  onReport?: (message: ChatMessage) => void;
  /** 长按消息触发撤回(仅自己的消息) */
  onRecall?: (message: ChatMessage) => void;
}

export default function ChatBubble({ message, isMine, avatar, nickname, onReport, onRecall }: Props) {
  const [preview, setPreview] = useState<string | null>(null);
  const [showMenu, setShowMenu] = useState(false);
  const navigate = useNavigate();
  const longPressTimer = useRef<ReturnType<typeof setTimeout>>();

  const handleAvatarClick = () => {
    if (message.fromUserId) {
      navigate(`/user/${message.fromUserId}`);
    }
  };

  // 长按 600ms 弹出操作菜单(撤回/举报)
  const startPress = () => {
    longPressTimer.current = setTimeout(() => {
      setShowMenu(true);
    }, 600);
  };
  const clearPress = () => {
    if (longPressTimer.current) clearTimeout(longPressTimer.current);
  };

  const recalled = message.isRecalled === 1;

  return (
    <>
      <div
        className={`chat-bubble-row ${isMine ? 'mine' : 'other'}`}
        onTouchStart={startPress}
        onTouchEnd={clearPress}
        onTouchMove={clearPress}
        onContextMenu={(e) => { e.preventDefault(); setShowMenu(true); }}
      >
        <div className="chat-avatar" onClick={handleAvatarClick} style={{ cursor: 'pointer' }}>
          {avatar ? <img src={avatar} alt="" /> : <AppIcon name="user" size={20} />}
        </div>
        <div className="chat-bubble-wrapper">
          {!isMine && <span className="chat-sender">{nickname}</span>}
          <div className={`chat-bubble ${isMine ? 'bubble-mine' : 'bubble-other'}${recalled ? ' bubble-recalled' : ''}`}>
            {recalled ? (
              <span style={{ color: '#bbb', fontStyle: 'italic' }}>该消息已撤回</span>
            ) : message.messageType === 'image' ? (
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

      {/* 长按操作菜单 */}
      {showMenu && (
        <div className="chat-menu-overlay" onClick={() => setShowMenu(false)}>
          <div className="chat-menu" onClick={(e) => e.stopPropagation()}>
            {isMine && (
              <button
                className="chat-menu-item"
                onClick={() => { setShowMenu(false); onRecall?.(message); }}
              >撤回</button>
            )}
            <button
              className="chat-menu-item"
              onClick={() => { setShowMenu(false); onReport?.(message); }}
            >举报</button>
          </div>
        </div>
      )}

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
