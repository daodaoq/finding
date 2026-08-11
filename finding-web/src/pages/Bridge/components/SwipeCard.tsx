import type { BridgeRecommendUser } from '../../../types/bridge';
import AppIcon from '../../../components/AppIcon';
import './SwipeCard.css';

interface Props {
  user: BridgeRecommendUser;
  onLike: () => void;
  onSkip: () => void;
  disabled?: boolean;
}

/**
 * 单卡推荐:一次展示一个用户,底部悬浮 爱心=喜欢 / 叉号=不感兴趣。
 * 采用多卡片结构:资料卡(主) + 介绍卡/标签卡/活跃卡(可选,按启用的字段渲染)。
 * 各字段按存在性渲染:候选人的卡片配置会隐藏未开启的字段(后端返回 null)。
 */
export default function SwipeCard({ user, onLike, onSkip, disabled }: Props) {
  const hasName = !!user.nickname;
  const hasMeta = !!(user.school || user.city || user.distanceKm != null);
  const hasIntro = !!user.signature;
  const hasTags = !!(user.matchReasons && user.matchReasons.length > 0);
  const hasOnline = !!user.lastLoginAt;
  const targetLabel = user.targetType === 1 ? '找对象'
    : user.targetType === 2 ? '交朋友' : '';

  return (
    <div className="swipe-wrap">
      {/* 资料卡(主) */}
      <div className="swipe-card">
        <div className="swipe-photo">
          {user.avatar ? (
            <img src={user.avatar} alt="" />
          ) : (
            <div className="swipe-photo-fallback" />
          )}
          {(hasName || hasMeta || !!targetLabel) && (
            <>
              <div className="swipe-photo-shade" />
              <div className="swipe-info-overlay">
                {hasName && (
                  <div className="swipe-name-row">
                    <span className="swipe-name">{user.nickname}</span>
                    {!!user.age && <span className="swipe-age">{user.age}</span>}
                    {!!user.gender && (
                      <span className={`swipe-gender ${user.gender === 1 ? 'male' : 'female'}`}>
                        {user.gender === 1 ? '♂' : '♀'}
                      </span>
                    )}
                    {user.verified === 1 && <span className="swipe-verified">已认证</span>}
                  </div>
                )}
                {hasMeta && (
                  <div className="swipe-meta">
                    {user.school && <span>{user.school}</span>}
                    <span>
                      {user.city || '未知城市'}
                      {user.distanceKm != null ? ` · ${fmtDistance(user.distanceKm)}` : ''}
                    </span>
                  </div>
                )}
                {!!targetLabel && <div className="swipe-target">{targetLabel}</div>}
              </div>
            </>
          )}
        </div>
      </div>

      {/* 介绍卡 */}
      {hasIntro && (
        <div className="swipe-subcard">
          <div className="swipe-subcard-title">自我介绍</div>
          <p className="swipe-bio">{user.signature}</p>
        </div>
      )}

      {/* 标签卡 */}
      {hasTags && (
        <div className="swipe-subcard">
          <div className="swipe-subcard-title">匹配理由</div>
          <div className="swipe-reasons">
            {user.matchReasons!.map((r) => <span key={r} className="swipe-reason-chip">{r}</span>)}
          </div>
        </div>
      )}

      {/* 活跃卡 */}
      {hasOnline && (
        <div className="swipe-subcard">
          <div className="swipe-subcard-title">在线状态</div>
          <p className="swipe-online">{fmtOnline(user.lastLoginAt)}</p>
        </div>
      )}

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
