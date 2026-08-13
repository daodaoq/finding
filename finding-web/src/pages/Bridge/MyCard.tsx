import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bridgeApi } from '../../api/bridge';
import { useAuthStore } from '../../store/authStore';
import { showToast } from '../../components/Toast';
import SwipeCard from './components/SwipeCard';
import type { BridgeRecommendUser, UserCardConfig } from '../../types/bridge';
import './MyCard.css';

/** 按卡片分组的展示项 */
const CARD_SECTIONS: { title: string; fields: { key: keyof UserCardConfig; label: string }[] }[] = [
  {
    title: '资料卡',
    fields: [
      { key: 'showPhoto', label: '照片' },
      { key: 'showNickname', label: '昵称' },
      { key: 'showAge', label: '年龄' },
      { key: 'showGender', label: '性别' },
      { key: 'showSchool', label: '学校' },
      { key: 'showCity', label: '城市' },
      { key: 'showDistance', label: '距离' },
      { key: 'showVerified', label: '认证标识' },
      { key: 'showTargetType', label: '交友目标' },
    ],
  },
  { title: '介绍卡', fields: [{ key: 'showSignature', label: '自我介绍' }] },
  { title: '标签卡', fields: [{ key: 'showMatchReasons', label: '匹配理由' }] },
  { title: '活跃卡', fields: [{ key: 'showLastOnline', label: '最近在线' }] },
];

const DEFAULT_CONFIG: UserCardConfig = {
  showPhoto: 1, showNickname: 1, showAge: 1, showGender: 1, showSchool: 1,
  showCity: 1, showDistance: 1, showVerified: 1, showTargetType: 1,
  showSignature: 1, showMatchReasons: 1, showLastOnline: 1,
};

function calcAge(birthday?: string): number | undefined {
  if (!birthday) return undefined;
  const b = new Date(birthday);
  if (Number.isNaN(b.getTime())) return undefined;
  const now = new Date();
  let age = now.getFullYear() - b.getFullYear();
  const m = now.getMonth() - b.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < b.getDate())) age--;
  return age >= 0 ? age : undefined;
}

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
    age: cfg.showAge ? calcAge(me?.birthday) : undefined,
    gender: cfg.showGender ? (me?.gender || 0) : 0,
    school: cfg.showSchool ? (me?.school || '') : '',
    city: cfg.showCity ? (me?.city || '') : '',
    distanceKm: undefined,
    verified: cfg.showVerified ? (me?.realNameVerified === 2 ? 1 : 0) : undefined,
    targetType: cfg.showTargetType ? (me?.targetType || 0) : undefined,
    signature: cfg.showSignature ? (me?.signature || '') : '',
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

      <p className="mycard-hint">别人在「相识」里看到的你的卡片，可组合多张卡片，选择要展示的信息</p>

      {/* 别人视角预览 */}
      <div className="mycard-preview">
        <SwipeCard user={previewUser} onLike={() => {}} onApply={() => {}} onSkip={() => {}} disabled />
      </div>
      {cfg.showMatchReasons === 1 && (
        <p className="mycard-note">* 匹配理由为示例，实际会按对方资料计算</p>
      )}

      {/* 展示项开关(按卡片分组) */}
      {CARD_SECTIONS.map((section) => (
        <div key={section.title} className="mycard-section">
          <div className="mycard-section-title">{section.title}</div>
          <div className="mycard-list">
            {section.fields.map((opt) => (
              <div key={opt.key} className="mycard-item">
                <div className="mycard-item-copy">
                  <b>{opt.label}</b>
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
        </div>
      ))}

      <button className="mycard-save" onClick={save} disabled={saving || !loaded}>
        {saving ? '保存中...' : '保存'}
      </button>
    </div>
  );
}
