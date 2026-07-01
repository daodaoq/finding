import type { Mate } from '../types/mate';
import { useAuthStore } from '../store/authStore';
import './MateCard.css';

interface Props {
  mate: Mate;
  onJoin: (id: number) => void;
  onClick: (id: number) => void;
}

export default function MateCard({ mate, onJoin, onClick }: Props) {
  const currentUserId = useAuthStore(s => s.user?.id);
  const isOwner = currentUserId != null && mate.userId === currentUserId;
  return (
    <div className="mate-card" onClick={() => onClick(mate.id)}>
      <div className="mate-card-top">
        <span className="mate-category">{mate.categoryDesc || mate.category}</span>
        <span className={`mate-status status-${mate.status}`}>
          {mate.status === 2 ? '已关闭' : mate.isFull ? '已满' : '招募中'}
        </span>
      </div>
      <h4 className="mate-title">{mate.title}</h4>
      <p className="mate-desc">{mate.description}</p>
      <div className="mate-meta">
        <span>🕐 {formatDateTime(mate.activityTime)}</span>
        <span>📍 {mate.location}</span>
        {mate.distanceKm != null && (
          <span>📏 {mate.distanceKm.toFixed(1)}km</span>
        )}
      </div>
      <div className="mate-card-bottom">
        <div className="mate-participants">
          <span>👥 {mate.currentParticipants}/{mate.maxParticipants}</span>
          {!mate.isAnonymous && mate.author && (
            <span className="mate-author">发起: {mate.author.nickname}</span>
          )}
        </div>
        {isOwner ? (
          <span className="owner-tag">我发布的</span>
        ) : (
          <>
            {!mate.hasJoined && mate.status === 1 && !mate.isFull && (
              <button className="join-btn" onClick={(e) => { e.stopPropagation(); onJoin(mate.id); }}>
                加入
              </button>
            )}
            {mate.hasJoined && <span className="joined-tag">已加入</span>}
          </>
        )}
      </div>
    </div>
  );
}

function formatDateTime(dateStr: string): string {
  const d = new Date(dateStr);
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
}
