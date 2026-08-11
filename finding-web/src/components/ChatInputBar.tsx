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
}

export default function ChatInputBar({ onSend, onUploading, mentionMembers }: Props) {
  const [text, setText] = useState('');
  const [panelOpen, setPanelOpen] = useState(false);
  const [showMentions, setShowMentions] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleSend = () => {
    if (!text.trim()) return;
    onSend(text.trim(), 'text');
    setText('');
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
    try {
      const res = await uploadApi.uploadImage(file);
      if (res.data.data) {
        onSend(res.data.data, 'image');
      }
    } catch {
      // 拦截器已提示
    } finally {
      onUploading?.(false);
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
          onChange={(e) => setText(e.target.value)}
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
