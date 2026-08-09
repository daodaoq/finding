import type { Ref } from 'react';
import './CommentInput.css';

interface Props {
  inputText: string;
  onInputChange: (v: string) => void;
  onSend: () => void;
  replyTo: { id: number; name: string } | null;
  onCancelReply: () => void;
  bottom: number;
  inputRef?: Ref<HTMLInputElement>;
}

/** 动态详情 - 底部评论输入栏（移动端键盘适配） */
export default function CommentInput({
  inputText,
  onInputChange,
  onSend,
  replyTo,
  onCancelReply,
  bottom,
  inputRef,
}: Props) {
  return (
    <div className="pd-input-bar" style={{ bottom: bottom > 0 ? bottom : 56 }}>
      {replyTo && (
        <div className="reply-indicator">
          回复 @{replyTo.name}
          <button onClick={onCancelReply}>✕</button>
        </div>
      )}
      <div className="pd-input-row">
        <input
          ref={inputRef}
          className="pd-input"
          type="text"
          placeholder={replyTo ? `回复 ${replyTo.name}...` : '说点什么...'}
          value={inputText}
          onChange={(e) => onInputChange(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') onSend(); }}
        />
        <button className="pd-send-btn" onClick={onSend} disabled={!inputText.trim()}>
          发送
        </button>
      </div>
    </div>
  );
}