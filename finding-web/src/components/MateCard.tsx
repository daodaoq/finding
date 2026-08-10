import type { Mate } from '../types/mate';
import { useAuthStore } from '../store/authStore';
import AppIcon from './AppIcon';
import './MateCard.css';

interface Props { mate: Mate; onJoin: (id: number) => void; onClick: (id: number) => void; }

export default function MateCard({ mate, onJoin, onClick }: Props) {
  const currentUserId = useAuthStore((state) => state.user?.id);
  const isOwner = currentUserId != null && mate.userId === currentUserId;
  const isEnded = mate.status === 2 || (mate.activityTime != null && new Date(mate.activityTime).getTime() < Date.now());
  const statusText = isEnded ? '已结束' : mate.isFull ? '已满' : '招募中';
  return <article className="mate-card" onClick={() => onClick(mate.id)}>
    <div className="mate-card-top"><span className="mate-category">{mate.categoryDesc || mate.category}</span><span className={`mate-status ${isEnded ? 'ended' : ''}`}>{statusText}</span></div>
    <h4 className="mate-title">{mate.title}</h4><p className="mate-desc">{mate.description}</p>
    <div className="mate-meta"><span><AppIcon name="calendar" size={15} />{formatDateTime(mate.activityTime)}</span><span><AppIcon name="location" size={15} />{mate.location}</span>{mate.distanceKm != null && <span>{mate.distanceKm.toFixed(1)} km</span>}</div>
    <div className="mate-card-bottom"><div className="mate-participants"><AppIcon name="users" size={15} />{mate.currentParticipants}/{mate.maxParticipants}{!mate.isAnonymous && mate.author && <span className="mate-author">发起人：{mate.author.nickname}</span>}</div>{isOwner ? <span className="owner-tag">我发布的</span> : !mate.hasJoined && mate.status === 1 && !mate.isFull ? <button className="join-btn" onClick={(event) => { event.stopPropagation(); onJoin(mate.id); }}>申请加入</button> : <span className="joined-tag">已加入</span>}</div>
  </article>;
}

function formatDateTime(dateStr: string): string { const date = new Date(dateStr); return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`; }
