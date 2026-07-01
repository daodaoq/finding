import { useState } from 'react';
import './ChatInputBar.css';

interface Props {
  onSend: (content: string) => void;
}

export default function ChatInputBar({ onSend }: Props) {
  const [text, setText] = useState('');

  const handleSend = () => {
    if (!text.trim()) return;
    onSend(text.trim());
    setText('');
  };

  return (
    <div className="chat-input-bar">
      <button className="input-action-btn">＋</button>
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
  );
}
