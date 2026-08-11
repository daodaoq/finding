import { useState, useRef } from 'react';
import { uploadApi } from '../api/upload';
import AppIcon from './AppIcon';
import './ChatInputBar.css';

interface MentionMember { userId: number; nickname: string; }

interface Props {
  onSend: (content: string, messageType?: string) => void;
  onUploading?: (uploading: boolean) => void;
  /** 传入时显示 @ 按钮,可 @ 群成员 */
  mentionMembers?: MentionMember[];
  /** 回复/引用目标(输入栏顶部显示引用条) */
  replyTo?: { id: number; content: string; nickname: string } | null;
  onCancelReply?: () => void;
  /** 输入变化时触发(父组件据此发送 typing 轻量事件,节流由本组件控制) */
  onTyping?: () => void;
}

export default function ChatInputBar({ onSend, onUploading, mentionMembers, replyTo, onCancelReply, onTyping }: Props) {
  const [text, setText] = useState('');
  const [panelOpen, setPanelOpen] = useState(false);
  const [showMentions, setShowMentions] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  const lastTypingAt = useRef(0);

  const handleSend = () => {
    if (!text.trim()) return;
    onSend(text.trim(), 'text');
    setText('');
  };

  /** 输入变化:节流触发 typing(每 2 秒最多一次) */
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setText(e.target.value);
    const now = Date.now();
    if (now - lastTypingAt.current > 2000) {
      lastTypingAt.current = now;
      onTyping?.();
    }
  };

  const handlePickImage = () => {
    setPanelOpen(false);
    // 延迟打开文件选择器，等面板动画结束
    setTimeout(() => fileRef.current?.click(), 150);
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    onUploading?.(true);
    setUploadProgress(0);
    try {
      const res = await uploadApi.uploadImage(file, (p) => setUploadProgress(p));
      if (res.data.data) {
        onSend(res.data.data, 'image');
      }
    } catch {
      // 拦截器已提示
    } finally {
      onUploading?.(false);
      setUploadProgress(null);
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  return (
    <div className="chat-input-area">
      {/* 操作面板 */}
      {panelOpen && (
        <>
          <div className="input-panel-mask" onClick={() => setPanelOpen(false)} />
          <div className="input-panel">
            <div className="panel-item" onClick={handlePickImage}>
              <span className="panel-icon"><AppIcon name="image" size={28} /></span>
              <span className="panel-label">图片</span>
            </div>
          </div>
        </>
      )}

      {/* @成员选择面板 */}
      {showMentions && mentionMembers && mentionMembers.length > 0 && (
        <div className="mention-panel">
          {mentionMembers.map((m) => (
            <div
              key={m.userId}
              className="mention-item"
              onClick={() => {
                setText((prev) => prev + '@' + m.nickname + ' ');
                setShowMentions(false);
              }}
            >
              @{m.nickname}
            </div>
          ))}
        </div>
      )}

      {/* 回复/引用条 */}
      {replyTo && (
        <div className="chat-reply-bar">
          <div className="chat-reply-info">
            <span className="chat-reply-name">回复 {replyTo.nickname}</span>
            <span className="chat-reply-content">{replyTo.content}</span>
          </div>
          <button className="chat-reply-cancel" onClick={onCancelReply}>✕</button>
        </div>
      )}

      {/* 图片上传进度 */}
      {uploadProgress !== null && (
        <div className="chat-upload-progress">
          <div className="chat-upload-bar" style={{ width: `${uploadProgress}%` }} />
          <span className="chat-upload-text">{uploadProgress < 100 ? `${uploadProgress}%` : '处理中…'}</span>
        </div>
      )}

      <div className="chat-input-bar">
        <input
          ref={fileRef}
          type="file"
          accept="image/*"
          style={{ display: 'none' }}
          onChange={handleFileChange}
        />
        <button className="input-action-btn" onClick={() => setPanelOpen(!panelOpen)}>
          {panelOpen ? '✕' : '＋'}
        </button>
        {mentionMembers && mentionMembers.length > 0 && (
          <button className="input-action-btn" onClick={() => setShowMentions(!showMentions)}>
            @
          </button>
        )}
        <input
          className="chat-input"
          type="text"
          placeholder="输入消息..."
          value={text}
          maxLength={2000}
          onChange={handleChange}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              handleSend();
            }
          }}
        />
        <button className="input-send-btn" onClick={handleSend} disabled={!text.trim()}>
          发送
        </button>
      </div>
    </div>
  );
}
