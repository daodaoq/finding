import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { formatClockTime } from '../utils/format';
import { openImagePreview, openVideoPreview } from '../utils/preview';
import { showToast } from './Toast';
import AppIcon from './AppIcon';
import './ChatBubble.css';

interface ChatMessage {
  id: number;
  fromUserId: number;
  content: string;
  messageType: string;
  isRecalled?: number;
  parentMessageId?: number;
  createdAt: string;
  /** 自己刚发送/失败的消息状态(失败可点击重试) */
  sendState?: 'sending' | 'sent' | 'failed';
}

interface Props {
  message: ChatMessage;
  isMine: boolean;
  avatar?: string;
  nickname?: string;
  /** 回复/引用:被回复消息(本地消息中查到的原文,可能为 null) */
  replyTo?: ChatMessage | null;
  replyToName?: string;
  /** 长按消息触发举报 */
  onReport?: (message: ChatMessage) => void;
  /** 长按消息触发撤回(仅自己的消息) */
  onRecall?: (message: ChatMessage) => void;
  /** 点击失败消息重试 */
  onRetry?: (message: ChatMessage) => void;
  /** 长按消息「回复」 */
  onReply?: (message: ChatMessage) => void;
}

export default function ChatBubble({
  message, isMine, avatar, nickname, replyTo, replyToName,
  onReport, onRecall, onRetry, onReply,
}: Props) {
  const [showMenu, setShowMenu] = useState(false);
  const navigate = useNavigate();
  const longPressTimer = useRef<ReturnType<typeof setTimeout>>();
  // 长按弹出菜单后,浏览器在 touchend 会补发 click;用时间戳吞掉这次 click,
  // 否则菜单与图片预览会同时打开(菜单层级更高,看起来像预览坏了)
  const suppressClickUntil = useRef(0);

  const handleAvatarClick = () => {
    if (message.fromUserId) {
      navigate(`/user/${message.fromUserId}`);
    }
  };

  // 长按 600ms 弹出操作菜单(撤回/举报)
  const startPress = () => {
    suppressClickUntil.current = 0; // 新手势重置,不误吞后续正常点击
    longPressTimer.current = setTimeout(() => {
      suppressClickUntil.current = Date.now() + 500;
      setShowMenu(true);
    }, 600);
  };
  const clearPress = () => {
    if (longPressTimer.current) clearTimeout(longPressTimer.current);
  };

  /** 媒体点击统一入口:长按刚触发过菜单则忽略这次 click */
  const tap = (fn: () => void) => () => {
    if (Date.now() < suppressClickUntil.current) return;
    fn();
  };

  // 卸载时清理长按定时器,避免消息滚动回收后仍弹出菜单
  useEffect(() => () => {
    if (longPressTimer.current) clearTimeout(longPressTimer.current);
  }, []);

  const recalled = message.isRecalled === 1;

  const copyMessage = async () => {
    try {
      await navigator.clipboard?.writeText(message.content);
      showToast('已复制');
    } catch {
      showToast('复制失败');
    }
  };

  return (
    <>
      <div
        className={`chat-bubble-row ${isMine ? 'mine' : 'other'}`}
        onTouchStart={startPress}
        onTouchEnd={clearPress}
        onTouchMove={clearPress}
        onContextMenu={(e) => {
          e.preventDefault();
          suppressClickUntil.current = Date.now() + 500; // 触控板 ctrl+点击也会补发 click
          setShowMenu(true);
        }}
      >
        <div className="chat-avatar" onClick={handleAvatarClick} style={{ cursor: 'pointer' }}>
          {avatar ? <img src={avatar} alt="" /> : <AppIcon name="user" size={20} />}
        </div>
        <div className="chat-bubble-wrapper">
          {!isMine && <span className="chat-sender">{nickname}</span>}
          <div className={`chat-bubble ${isMine ? 'bubble-mine' : 'bubble-other'}${recalled ? ' bubble-recalled' : ''}`}>
            {replyTo && !recalled && (
              <div className="chat-quote">
                <span className="chat-quote-name">{replyToName || '引用'}</span>
                <span className="chat-quote-content">
                  {replyTo.isRecalled === 1 ? '原消息已撤回'
                    : replyTo.messageType === 'image' ? '[图片]'
                    : replyTo.messageType === 'video' ? '[视频]' : replyTo.content}
                </span>
              </div>
            )}
            {recalled ? (
              <span style={{ color: '#bbb', fontStyle: 'italic' }}>该消息已撤回</span>
            ) : message.messageType === 'image' ? (
              <img
                src={message.content}
                alt=""
                className="chat-image"
                onClick={tap(() => openImagePreview(message.content))}
              />
            ) : message.messageType === 'video' ? (
              <div className="chat-video" onClick={tap(() => openVideoPreview(message.content))}>
                <video src={message.content} muted playsInline preload="metadata" />
                <div className="chat-video-play">
                  <span><AppIcon name="video" size={20} /></span>
                </div>
              </div>
            ) : (
              <span>{message.content}</span>
            )}
          </div>
          <span className="chat-time">{formatClockTime(message.createdAt)}</span>
          {isMine && message.sendState === 'failed' && (
            <button
              className="chat-retry"
              onClick={(e) => { e.stopPropagation(); onRetry?.(message); }}
            >
              发送失败·点击重试
            </button>
          )}
        </div>
      </div>

      {/* 长按操作菜单 */}
      {showMenu && (
        <div className="chat-menu-overlay" onClick={() => setShowMenu(false)}>
          <div className="chat-menu" onClick={(e) => e.stopPropagation()}>
            {avatar && (
              <button
                className="chat-menu-item"
                onClick={() => { setShowMenu(false); openImagePreview(avatar); }}
              >查看头像</button>
            )}
            <button
              className="chat-menu-item"
              onClick={() => { setShowMenu(false); copyMessage(); }}
            >复制</button>
            {onReply && !recalled && (
              <button
                className="chat-menu-item"
                onClick={() => { setShowMenu(false); onReply(message); }}
              >回复</button>
            )}
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

      {/* 图片/视频预览已统一到全局 ImagePreviewHost(MainLayout 挂载) */}
    </>
  );
}
