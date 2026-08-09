import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { settingsApi } from '../../../api/settings';
import { showToast } from '../../../components/Toast';
import '../subpage.css';
import './settings.css';

const MODES = [
  { value: 0, label: '所有人可申请', desc: '任何人申请后自动通过，直接建立聊天' },
  { value: 1, label: '需验证（默认）', desc: '对方申请后需要你同意' },
  { value: 2, label: '不允许申请', desc: '拒绝所有人的好友申请' },
];

export default function FriendSetting() {
  const navigate = useNavigate();
  const [mode, setMode] = useState(1);

  useEffect(() => {
    settingsApi.get()
      .then((res) => setMode(res.data.data.friendAddMode ?? 1))
      .catch(() => {});
  }, []);

  const pick = async (value: number) => {
    setMode(value);
    try {
      await settingsApi.update({ friendAddMode: value });
      showToast('已保存');
    } catch { showToast('保存失败'); }
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
