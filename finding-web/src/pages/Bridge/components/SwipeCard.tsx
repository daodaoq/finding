import type { BridgeRecommendUser } from '../../../types/bridge';
import AppIcon from '../../../components/AppIcon';
import './SwipeCard.css';

interface Props {
  user: BridgeRecommendUser;
  onLike: () => void;
  onSkip: () => void;
  disabled?: boolean;
}

/** 单卡推荐:一次展示一个用户,底部悬浮 爱心=喜欢 / 叉号=不感兴趣 */
export default function SwipeCard({ user, onLike, onSkip, disabled }: Props) {
  return (
    <div className="swipe-wrap">
      <div className="swipe-card">
        {/* 照片区(头像为主图) */}
        <div className="swipe-photo">
          {user.avatar ? (
            <img src={user.avatar} alt="" />
          ) : (
            <div className="swipe-photo-fallback"><AppIcon name="user" size={64} /></div>
          )}
          <div className="swipe-photo-shade" />
          {/* 照片底部信息浮层 */}
          <div className="swipe-info-overlay">
            <div className="swipe-name-row">
              <span className="swipe-name">{user.nickname || '用户'}</span>
              <span className={`swipe-gender ${user.gender === 1 ? 'male' : 'female'}`}>
                {user.gender === 1 ? '♂' : '♀'}
              </span>
            </div>
            <div className="swipe-meta">
              {user.school && <span>{user.school}</span>}
              <span>
                {user.city || '未知城市'}
                {user.distanceKm != null ? ` · ${fmtDistance(user.distanceKm)}` : ''}
              </span>
            </div>
          </div>
        </div>

        {/* 公开自我介绍区 */}
        <div className="swipe-body">
          {user.signature ? (
            <p className="swipe-bio">{user.signature}</p>
          ) : (
            <p className="swipe-bio swipe-bio--empty">这个人很懒，还没有写自我介绍</p>
          )}
          {user.matchReasons && user.matchReasons.length > 0 && (
            <div className="swipe-reasons">
              {user.matchReasons.map((r) => <span key={r} className="swipe-reason-chip">{r}</span>)}
            </div>
          )}
          <p className="swipe-online">{fmtOnline(user.lastLoginAt)}</p>
        </div>
      </div>

      {/* 悬浮操作按钮 */}
      <div className="swipe-actions">
        <button className="swipe-btn swipe-btn--skip" onClick={onSkip} disabled={disabled} aria-label="不感兴趣">
          <AppIcon name="x" size={30} />
        </button>
        <button className="swipe-btn swipe-btn--like" onClick={onLike} disabled={disabled} aria-label="喜欢">
          <AppIcon name="heart" size={28} />
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
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 60000) return '刚刚在线';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前在线`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前在线`;
  return `${Math.floor(diff / 86400000)}天前在线`;
}
