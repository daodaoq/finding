import { useState, useRef } from 'react';
import './ChatInputBar.css';

interface Props {
  onSend: (content: string, messageType?: string) => void;
  onUploading?: (uploading: boolean) => void;
}

export default function ChatInputBar({ onSend, onUploading }: Props) {
  const [text, setText] = useState('');
  const [panelOpen, setPanelOpen] = useState(false);
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
      const fd = new FormData();
      fd.append('file', file);
      const res = await fetch('/api/v1/upload/image', {
        method: 'POST',
        headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
        body: fd,
      });
      const json = await res.json();
      if (json.code === 200 && json.data) {
        onSend(json.data, 'image');
      }
    } catch {
      // ignore
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
              <span className="panel-icon">🖼️</span>
              <span className="panel-label">图片</span>
            </div>
          </div>
        </>
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
        <input
          className="chat-input"
          type="text"
          placeholder="输入消息..."
          value={text}
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
