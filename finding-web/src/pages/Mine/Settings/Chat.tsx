import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { settingsApi } from '../../../api/settings';
import { uploadApi } from '../../../api/upload';
import { CHAT_BG_PRESETS, resolveChatBg } from '../../../utils/chatBackgrounds';
import { showToast } from '../../../components/Toast';
import '../subpage.css';
import './settings.css';

/** 聊天通用 —— 全局聊天设置,可被单个聊天框的设置覆盖 */
export default function ChatSettingsGlobal() {
  const navigate = useNavigate();
  const fileRef = useRef<HTMLInputElement>(null);
  const [chatBg, setChatBg] = useState<string | null>(null);
  const [chatMuted, setChatMuted] = useState(0);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    settingsApi.get()
      .then((res) => {
        setChatBg(res.data.data.chatBg ?? null);
        setChatMuted(res.data.data.chatMuted ?? 0);
      })
      .catch(() => {});
  }, []);

  const update = async (patch: Record<string, unknown>) => {
    try {
      await settingsApi.update(patch);
      showToast('已保存');
    } catch { showToast('保存失败'); }
  };

  const pickBg = (key: string) => {
    setChatBg(key);
    update({ chatBg: key });
  };

  const resetBg = () => {
    setChatBg(null);
    update({ chatBg: '' });
  };

  const uploadBg = async (file: File) => {
    setUploading(true);
    try {
      const res = await uploadApi.uploadImage(file);
      setChatBg(res.data.data);
      update({ chatBg: res.data.data });
    } catch { showToast('上传失败'); }
    finally { setUploading(false); }
  };

  const toggleMuted = () => {
    const v = chatMuted === 1 ? 0 : 1;
    setChatMuted(v);
    update({ chatMuted: v });
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine/settings')}>←</button>
        <h2>聊天通用</h2>
      </div>

      {/* 全局免打扰 */}
      <div className="set-card">
        <div className="set-row">
          <div>
            <div className="set-label">全局消息免打扰</div>
            <div className="set-desc">开启后默认所有聊天不推送，单个聊天可单独覆盖</div>
          </div>
          <span className={`set-switch ${chatMuted === 1 ? 'on' : ''}`} onClick={toggleMuted}>
            <span className="set-switch-dot" />
          </span>
        </div>
      </div>

      {/* 全局聊天背景 */}
      <div className="set-card">
        <div className="set-row">
          <div>
            <div className="set-label">全局默认聊天背景</div>
            <div className="set-desc">未单独设置背景的聊天将使用此背景</div>
          </div>
        </div>
        <div className="bg-grid">
          {Object.entries(CHAT_BG_PRESETS).map(([key, style]) => (
            <div
              key={key}
              className={`bg-cell ${chatBg === key ? 'active' : ''}`}
              style={resolveChatBg(key)}
              onClick={() => pickBg(key)}
            />
          ))}
        </div>
        <button className="bg-upload-btn" onClick={() => fileRef.current?.click()}>
          {uploading ? '上传中...' : '📷 上传图片背景'}
        </button>
        <button className="bg-reset" onClick={resetBg}>恢复默认背景</button>
        <input
          ref={fileRef}
          type="file"
          accept="image/*"
          style={{ display: 'none' }}
          onChange={(e) => { const f = e.target.files?.[0]; if (f) uploadBg(f); e.target.value = ''; }}
        />
      </div>
    </div>
  );
}
