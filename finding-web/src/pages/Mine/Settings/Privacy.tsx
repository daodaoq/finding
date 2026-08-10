import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { settingsApi } from '../../../api/settings';
import { showToast } from '../../../components/Toast';
import '../subpage.css';
import './settings.css';

const VISIBLE_OPTIONS = [
  { value: 1, label: '所有人可见', desc: '任何人可查看你的主页资料' },
  { value: 2, label: '仅已互换信息', desc: '未互换者只能看到头像、昵称、学校等公开资料' },
];

export default function PrivacySetting() {
  const navigate = useNavigate();
  const [searchable, setSearchable] = useState(1);
  const [profileVisible, setProfileVisible] = useState(1);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    settingsApi.get()
      .then((res) => {
        setSearchable(res.data.data.searchable ?? 1);
        setProfileVisible(res.data.data.profileVisible ?? 1);
      })
      .catch(() => {});
  }, []);

  /** 保存设置;失败回滚到旧值,并防止连点并发提交 */
  const update = async (patch: Record<string, unknown>, rollback: () => void) => {
    if (saving) return;
    setSaving(true);
    try {
      await settingsApi.update(patch);
      showToast('已保存');
    } catch {
      rollback();
      showToast('保存失败');
    } finally {
      setSaving(false);
    }
  };

  const toggleSearchable = () => {
    if (saving) return;
    const prev = searchable;
    const v = searchable === 1 ? 0 : 1;
    setSearchable(v);
    update({ searchable: v }, () => setSearchable(prev));
  };

  const changeVisible = (v: number) => {
    if (saving) return;
    const prev = profileVisible;
    setProfileVisible(v);
    update({ profileVisible: v }, () => setProfileVisible(prev));
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
            <div className="set-desc">关闭后不会出现在用户搜索和相亲推荐中</div>
          </div>
          <span
            className={`set-switch ${searchable === 1 ? 'on' : ''}`}
            onClick={toggleSearchable}
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
          <div key={o.value} className="set-option" onClick={() => changeVisible(o.value)}>
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
