import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { settingsApi } from '../../../api/settings';
import { showToast } from '../../../components/Toast';
import '../subpage.css';
import './settings.css';

const VISIBLE_OPTIONS = [
  { value: 1, label: '所有人可见', desc: '任何人可查看你的主页资料' },
  { value: 2, label: '仅已互换信息', desc: '只有互换过详细信息的人可查看（预留）' },
];

export default function PrivacySetting() {
  const navigate = useNavigate();
  const [searchable, setSearchable] = useState(1);
  const [profileVisible, setProfileVisible] = useState(1);

  useEffect(() => {
    settingsApi.get()
      .then((res) => {
        setSearchable(res.data.data.searchable ?? 1);
        setProfileVisible(res.data.data.profileVisible ?? 1);
      })
      .catch(() => {});
  }, []);

  const update = async (patch: Record<string, unknown>) => {
    try {
      await settingsApi.update(patch);
      showToast('已保存');
    } catch { showToast('保存失败'); }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine/settings')}>←</button>
        <h2>个人权限</h2>
      </div>

      <div className="set-card">
        <div className="set-row">
          <div>
            <div className="set-label">允许被搜索</div>
            <div className="set-desc">关闭后其他同学无法在搜索中找到你</div>
          </div>
          <span
            className={`set-switch ${searchable === 1 ? 'on' : ''}`}
            onClick={() => { const v = searchable === 1 ? 0 : 1; setSearchable(v); update({ searchable: v }); }}
          >
            <span className="set-switch-dot" />
          </span>
        </div>
      </div>

      <div className="set-card">
        <div className="set-row">
          <div className="set-label">主页可见性</div>
        </div>
        {VISIBLE_OPTIONS.map((o) => (
          <div key={o.value} className="set-option" onClick={() => { setProfileVisible(o.value); update({ profileVisible: o.value }); }}>
            <div>
              <div className="set-option-label">{o.label}</div>
              <div className="set-desc">{o.desc}</div>
            </div>
            <span className={`set-radio ${profileVisible === o.value ? 'active' : ''}`} />
          </div>
        ))}
      </div>
    </div>
  );
}
