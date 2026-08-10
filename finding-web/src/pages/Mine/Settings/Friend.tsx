import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { settingsApi } from '../../../api/settings';
import { showToast } from '../../../components/Toast';
import '../subpage.css';
import './settings.css';

const MODES = [
  { value: 0, label: '自动同意', desc: '任何人申请后自动通过，直接建立聊天' },
  { value: 1, label: '需对方同意（默认）', desc: '对方申请后需要你同意' },
  { value: 2, label: '拒绝新的聊天申请', desc: '拒绝所有人的申请，已有会话不受影响' },
];

export default function FriendSetting() {
  const navigate = useNavigate();
  const [mode, setMode] = useState(1);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    settingsApi.get()
      .then((res) => setMode(res.data.data.friendAddMode ?? 1))
      .catch(() => {});
  }, []);

  /** 保存设置;失败回滚到旧值,并防止连点并发提交 */
  const pick = async (value: number) => {
    if (saving) return;
    const prev = mode;
    setMode(value);
    setSaving(true);
    try {
      await settingsApi.update({ friendAddMode: value });
      showToast('已保存');
    } catch {
      setMode(prev);
      showToast('保存失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine/settings')}>←</button>
        <h2>加好友方式</h2>
      </div>
      <div className="set-card">
        {MODES.map((m) => (
          <div key={m.value} className="set-option" onClick={() => pick(m.value)}>
            <div>
              <div className="set-option-label">{m.label}</div>
              <div className="set-desc">{m.desc}</div>
            </div>
            <span className={`set-radio ${mode === m.value ? 'active' : ''}`} />
          </div>
        ))}
      </div>
      <p className="set-hint">控制其他同学能否、以及如何申请加你为好友</p>
    </div>
  );
}
