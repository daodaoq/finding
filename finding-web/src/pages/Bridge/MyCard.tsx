import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bridgeApi } from '../../api/bridge';
import { useAuthStore } from '../../store/authStore';
import { showToast } from '../../components/Toast';
import SwipeCard from './components/SwipeCard';
import type { BridgeRecommendUser, UserCardConfig } from '../../types/bridge';
import './MyCard.css';

const FIELD_OPTIONS: { key: keyof UserCardConfig; label: string; desc?: string }[] = [
  { key: 'showPhoto', label: '照片', desc: '主展示图' },
  { key: 'showNickname', label: '昵称' },
  { key: 'showGender', label: '性别' },
  { key: 'showSchool', label: '学校' },
  { key: 'showCity', label: '城市' },
  { key: 'showDistance', label: '距离' },
  { key: 'showSignature', label: '自我介绍' },
  { key: 'showMatchReasons', label: '匹配理由' },
  { key: 'showLastOnline', label: '最近在线' },
];

const DEFAULT_CONFIG: UserCardConfig = {
  showPhoto: 1, showNickname: 1, showGender: 1, showSchool: 1, showCity: 1,
  showDistance: 1, showSignature: 1, showMatchReasons: 1, showLastOnline: 1,
};

export default function MyCardPage() {
  const navigate = useNavigate();
  const me = useAuthStore((s) => s.user);
  const [cfg, setCfg] = useState<UserCardConfig>(DEFAULT_CONFIG);
  const [saving, setSaving] = useState(false);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    bridgeApi.getCardConfig()
      .then((res) => setCfg({ ...DEFAULT_CONFIG, ...res.data.data }))
      .catch(() => showToast('加载配置失败'))
      .finally(() => setLoaded(true));
  }, []);

  const toggle = (key: keyof UserCardConfig) => {
    setCfg((prev) => ({ ...prev, [key]: prev[key] ? 0 : 1 }));
  };

  const save = async () => {
    setSaving(true);
    try {
      await bridgeApi.updateCardConfig(cfg);
      showToast('卡片配置已保存');
    } catch (e: any) {
      showToast((e as Error)?.message || '保存失败');
    } finally {
      setSaving(false);
    }
  };

  // 预览:别人看到的我的卡片(按当前开关裁剪字段)
  const previewUser: BridgeRecommendUser = {
    userId: me?.id || 0,
    nickname: cfg.showNickname ? (me?.nickname || '') : '',
    avatar: cfg.showPhoto ? (me?.avatar || '') : '',
    gender: cfg.showGender ? (me?.gender || 0) : 0,
    school: cfg.showSchool ? (me?.school || '') : '',
    city: cfg.showCity ? (me?.city || '') : '',
    signature: cfg.showSignature ? (me?.signature || '') : '',
    distanceKm: undefined,
    lastLoginAt: cfg.showLastOnline ? (me?.lastLoginAt || '') : '',
    isLiked: false,
    mutualFriends: 0,
    matchReasons: cfg.showMatchReasons ? ['同校', '已认证'] : undefined,
  };

  return (
    <div className="mycard-page">
      <div className="mycard-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h2>我的卡片</h2>
      </div>

      <p className="mycard-hint">别人在「相识」里看到的你的卡片，选择要展示的信息</p>

      {/* 别人视角预览 */}
      <div className="mycard-preview">
        <SwipeCard user={previewUser} onLike={() => {}} onSkip={() => {}} disabled />
      </div>
      {cfg.showMatchReasons === 1 && (
        <p className="mycard-note">* 匹配理由为示例，实际会按对方资料计算</p>
      )}

      {/* 展示项开关 */}
      <div className="mycard-list">
        {FIELD_OPTIONS.map((opt) => (
          <div key={opt.key} className="mycard-item">
            <div className="mycard-item-copy">
              <b>{opt.label}</b>
              {opt.desc && <small>{opt.desc}</small>}
            </div>
            <button
              className={`mycard-switch ${cfg[opt.key] ? 'on' : ''}`}
              onClick={() => toggle(opt.key)}
              aria-label={opt.label}
              aria-pressed={!!cfg[opt.key]}
            >
              <span className="mycard-switch-dot" />
            </button>
          </div>
        ))}
      </div>

      <button className="mycard-save" onClick={save} disabled={saving || !loaded}>
        {saving ? '保存中...' : '保存'}
      </button>
    </div>
  );
}
