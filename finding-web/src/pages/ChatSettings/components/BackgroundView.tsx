import { useRef } from 'react';
import { CHAT_BG_PRESETS, resolveChatBg } from '../../../utils/chatBackgrounds';
import './BackgroundView.css';

interface Props {
  background: string | null;
  onUpdate: (bg: string) => void;
  onUpload: (file: File) => void;
  onBack: () => void;
}

/** 聊天信息页 - 设置当前聊天背景视图 */
export default function BackgroundView({ background, onUpdate, onUpload, onBack }: Props) {
  const bgFileRef = useRef<HTMLInputElement>(null);

  const handlePick = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      await onUpload(file);
    } finally {
      e.target.value = '';
    }
  };

  return (
    <div className="cs-page">
      <div className="cs-header">
        <button className="back-btn" onClick={onBack}>←</button>
        <span>设置聊天背景</span>
      </div>
      <div className="cs-bg-body">
        <div className="cs-bg-current" style={resolveChatBg(background) || { background: '#f0f0f0' }}>
          <span>当前背景</span>
        </div>
        <div className="cs-bg-grid">
          {Object.entries(CHAT_BG_PRESETS).map(([key, style]) => (
            <button
              key={key}
              className={`cs-bg-swatch ${background === key ? 'active' : ''}`}
              style={style}
              onClick={() => onUpdate(key)}
            />
          ))}
        </div>
        <div className="cs-bg-actions">
          <button className="cs-bg-action" onClick={() => onUpdate('')}>
            恢复默认
          </button>
          <button className="cs-bg-action" onClick={() => bgFileRef.current?.click()}>
            上传图片
          </button>
          <input ref={bgFileRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={handlePick} />
        </div>
      </div>
    </div>
  );
}