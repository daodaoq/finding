import { useState } from 'react';
import type { BridgeRecommendUser } from '../../../types/bridge';
import AppIcon from '../../../components/AppIcon';
import './SwipeCard.css';

/** 匹配理由 → 简短解释(「为什么推荐我」面板用) */
const REASON_EXPLAIN: Record<string, string> = {
  '同校': '你们来自同一所学校',
  '同城': '你们在同一个城市',
  '已认证': 'TA 已通过学生认证',
  '近期活跃': 'TA 最近比较活跃',
  '兴趣相投': '你们的签名有共同话题',
  '距离较近': 'TA 离你很近',
  '偏好同校': '你申请过的人多来自这所学校',
  '偏好同城': '你申请过的人多在这个城市',
};

interface Props {
  user: BridgeRecommendUser;
  onLike: () => void;
  onApply: () => void;
  onSkip: () => void;
  disabled?: boolean;
}

export default function SwipeCard({ user, onLike, onApply, onSkip, disabled }: Props) {
  const [showWhy, setShowWhy] = useState(false);
  const hasName = !!user.nickname;
  const hasMeta = !!(user.school || user.city || user.distanceKm != null);
  const hasIntro = !!user.signature;
  const hasTags = !!(user.matchReasons && user.matchReasons.length > 0);
  const hasOnline = !!user.lastLoginAt;
  const targetLabel = user.targetType === 1 ? '找对象' : user.targetType === 2 ? '交朋友' : '';

  return (
    <div className="swipe-wrap">
      <div className="swipe-card">
        <div className="swipe-photo">
          {user.avatar ? <img src={user.avatar} alt="" /> : <div className="swipe-photo-fallback" />}
          {(hasName || hasMeta || !!targetLabel) && (
            <>
              <div className="swipe-photo-shade" />
              <div className="swipe-info-overlay">
                {hasName && (
                  <div className="swipe-name-row">
                    <span className="swipe-name">{user.nickname}</span>
                    {!!user.age && <span className="swipe-age">{user.age}岁</span>}
                    {!!user.gender && (
                      <span className={`swipe-gender ${user.gender === 1 ? 'male' : 'female'}`}>
                        {user.gender === 1 ? '男' : '女'}
                      </span>
                    )}
                    {user.verified === 1 && <span className="swipe-verified">已认证</span>}
                  </div>
                )}
                {hasMeta && (
                  <div className="swipe-meta">
                    {user.school && <span>{user.school}</span>}
                    <span>{user.city || '未知城市'}{user.distanceKm != null ? ` · ${fmtDistance(user.distanceKm)}` : ''}</span>
                  </div>
                )}
                {!!targetLabel && <div className="swipe-target">{targetLabel}</div>}
              </div>
            </>
          )}
        </div>
      </div>

      {(hasIntro || hasTags || hasOnline) && (
        <div className="swipe-details-card">
          {hasIntro && (
            <section className="swipe-detail-section swipe-detail-intro">
              <div className="swipe-subcard-title">自我介绍</div>
              <p className="swipe-bio">{user.signature}</p>
            </section>
          )}
          {(hasTags || hasOnline) && (
            <div className="swipe-detail-grid">
              {hasTags && (
                <section className="swipe-detail-section">
                  <div className="swipe-subcard-title">匹配理由</div>
                  <div className="swipe-reasons">
                    {user.matchReasons!.map((reason) => <span key={reason} className="swipe-reason-chip">{reason}</span>)}
                  </div>
                  <button className="swipe-why-btn" onClick={() => setShowWhy(v => !v)}>
                    {showWhy ? '收起解释' : '为什么推荐我 ›'}
                  </button>
                  {showWhy && (
                    <ul className="swipe-why-list">
                      {user.matchReasons!.map((reason) => (
                        <li key={reason}>
                          <b>{reason}</b>
                          <span>{REASON_EXPLAIN[reason] || '根据双方资料综合判断'}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </section>
              )}
              {hasOnline && (
                <section className="swipe-detail-section swipe-detail-online">
                  <div className="swipe-subcard-title">在线状态</div>
                  <p className={`swipe-online ${user.online ? 'is-online' : ''}`}>
                    {user.online ? '在线' : fmtOnline(user.lastLoginAt)}
                  </p>
                </section>
              )}
            </div>
          )}
        </div>
      )}

      <div className="swipe-actions">
        <button className="swipe-btn swipe-btn--skip" onClick={onSkip} disabled={disabled} aria-label="不感兴趣">
          <AppIcon name="x" size={28} />
        </button>
        <button className="swipe-btn swipe-btn--apply" onClick={onApply} disabled={disabled} aria-label="打招呼">
          <AppIcon name="send" size={26} />
        </button>
        <button
          className={`swipe-btn swipe-btn--like ${user.liked ? 'liked' : ''}`}
          onClick={onLike} disabled={disabled} aria-label="心动">
          <AppIcon name="heart" size={27} />
        </button>
      </div>
    </div>
  );
}

function fmtDistance(km: number): string {
  if (km < 1) return `${Math.round(km * 10) / 10}km`;
  return `${Math.round(km)}km`;
}

function fmtOnline(dateStr?: string): string {
  if (!dateStr) return '';
  const diff = new Date().getTime() - new Date(dateStr).getTime();
  if (diff < 60000) return '刚刚在线';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前在线`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前在线`;
  return `${Math.floor(diff / 86400000)}天前在线`;
}
