import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bridgeApi, type UserMatchPreference } from '../../../api/bridge';
import { showToast } from '../../../components/Toast';
import '../subpage.css';
import './settings.css';

const GENDER_OPTIONS = [
  { value: 0, label: '不限' },
  { value: 1, label: '男' },
  { value: 2, label: '女' },
];

const TARGET_OPTIONS = [
  { value: 0, label: '不限' },
  { value: 1, label: '找对象' },
  { value: 2, label: '交朋友' },
];

export default function PreferenceSetting() {
  const navigate = useNavigate();
  const [pref, setPref] = useState<UserMatchPreference>({
    preferGender: 0, minAge: 0, maxAge: 0, maxDistanceKm: 0, onlyVerified: 0,
    preferTargetType: 0, minCompleteness: 0,
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    bridgeApi.getPreference()
      .then((res) => {
        const d = res.data.data || {};
        setPref({
          preferGender: d.preferGender ?? 0,
          minAge: d.minAge ?? 0,
          maxAge: d.maxAge ?? 0,
          maxDistanceKm: d.maxDistanceKm ?? 0,
          onlyVerified: d.onlyVerified ?? 0,
          preferTargetType: d.preferTargetType ?? 0,
          minCompleteness: d.minCompleteness ?? 0,
        });
      })
      .catch(() => {});
  }, []);

  const set = (patch: Partial<UserMatchPreference>) => setPref((prev) => ({ ...prev, ...patch }));

  const save = async (patch: Partial<UserMatchPreference>) => {
    if (saving) return;
    const next = { ...pref, ...patch };
    setPref(next);
    setSaving(true);
    try {
      await bridgeApi.updatePreference(next);
      showToast('已保存');
    } catch { showToast('保存失败'); }
    finally { setSaving(false); }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine/settings')}>←</button>
        <h2>相亲偏好</h2>
      </div>

      <div className="set-card">
        <div className="set-row">
          <div className="set-label">偏好性别</div>
        </div>
        <div style={{ display: 'flex', gap: 8, padding: '0 16px 12px' }}>
          {GENDER_OPTIONS.map((g) => (
            <button
              key={g.value}
              className={`pe-gender-chip ${pref.preferGender === g.value ? 'active' : ''}`}
              style={{ flex: 1, border: 'none', padding: '8px 0', borderRadius: 14, fontSize: 13, background: pref.preferGender === g.value ? '#29241f' : '#f0f0f0', color: pref.preferGender === g.value ? '#fff' : '#666' }}
              onClick={() => save({ preferGender: g.value })}
            >{g.label}</button>
          ))}
        </div>
      </div>

      <div className="set-card">
        <div className="set-row">
          <div className="set-label">偏好目标</div>
        </div>
        <div style={{ display: 'flex', gap: 8, padding: '0 16px 12px' }}>
          {TARGET_OPTIONS.map((t) => (
            <button
              key={t.value}
              style={{ flex: 1, border: 'none', padding: '8px 0', borderRadius: 14, fontSize: 13, background: pref.preferTargetType === t.value ? '#29241f' : '#f0f0f0', color: pref.preferTargetType === t.value ? '#fff' : '#666' }}
              onClick={() => save({ preferTargetType: t.value })}
            >{t.label}</button>
          ))}
        </div>
      </div>

      <div className="set-card">
        <div className="set-row">
          <div className="set-label">资料完整度最低门槛</div>
          <span className="set-desc">0-10,0 表示不限</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '0 16px 14px' }}>
          <input
            type="number" min={0} max={10} value={pref.minCompleteness}
            onChange={(e) => set({ minCompleteness: Number(e.target.value) || 0 })}
            onBlur={() => save({ minCompleteness: pref.minCompleteness })}
            style={{ flex: 1, border: '1px solid #eee', borderRadius: 8, padding: '8px 10px', fontSize: 14 }}
          />
        </div>
      </div>

      <div className="set-card">
        <div className="set-row">
          <div className="set-label">年龄范围</div>
          <span className="set-desc">0 表示不限</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '0 16px 14px' }}>
          <input
            type="number" min={0} max={100} value={pref.minAge}
            onChange={(e) => set({ minAge: Number(e.target.value) || 0 })}
            onBlur={() => save({ minAge: pref.minAge })}
            style={{ flex: 1, border: '1px solid #eee', borderRadius: 8, padding: '8px 10px', fontSize: 14 }}
          />
          <span style={{ color: '#999', fontSize: 13 }}>~</span>
          <input
            type="number" min={0} max={100} value={pref.maxAge}
            onChange={(e) => set({ maxAge: Number(e.target.value) || 0 })}
            onBlur={() => save({ maxAge: pref.maxAge })}
            style={{ flex: 1, border: '1px solid #eee', borderRadius: 8, padding: '8px 10px', fontSize: 14 }}
          />
          <span style={{ color: '#999', fontSize: 13 }}>岁</span>
        </div>
      </div>

      <div className="set-card">
        <div className="set-row">
          <div className="set-label">最大距离</div>
          <span className="set-desc">0 表示不限</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '0 16px 14px' }}>
          <input
            type="number" min={0} value={pref.maxDistanceKm}
            onChange={(e) => set({ maxDistanceKm: Number(e.target.value) || 0 })}
            onBlur={() => save({ maxDistanceKm: pref.maxDistanceKm })}
            style={{ flex: 1, border: '1px solid #eee', borderRadius: 8, padding: '8px 10px', fontSize: 14 }}
          />
          <span style={{ color: '#999', fontSize: 13 }}>km</span>
        </div>
      </div>

      <div className="set-card">
        <div className="set-row">
          <div>
            <div className="set-label">只看已认证用户</div>
            <div className="set-desc">只推荐完成学生认证的用户</div>
          </div>
          <span
            className={`set-switch ${pref.onlyVerified === 1 ? 'on' : ''}`}
            onClick={() => save({ onlyVerified: pref.onlyVerified === 1 ? 0 : 1 })}
          >
            <span className="set-switch-dot" />
          </span>
        </div>
      </div>

      <p className="set-hint" style={{ textAlign: 'center', padding: '16px' }}>
        相亲推荐会按你的偏好过滤候选人,并展示主要匹配理由
      </p>
    </div>
  );
}
