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
  return <article className="mate-card" onClick={() => onClick(mate.id)}><div className="mate-card-cover"><img src={`https://picsum.photos/seed/${encodeURIComponent(`${mate.category}-${mate.id}`)}/240/240`} alt="" /><span className={`mate-card-state ${isEnded ? 'is-ended' : ''}`}>{statusText}</span></div><div className="mate-card-content"><div className="mate-card-title-row"><h3>{mate.title}</h3><span className="mate-card-price">免费</span></div><div className="mate-card-tags"><span>{mate.categoryDesc || mate.category}</span>{mate.isAnonymous ? <span>匿名活动</span> : null}</div><p className="mate-card-description">{mate.description || '一起认识新朋友，度过一段轻松的校园时光。'}</p><div className="mate-card-meta"><span><AppIcon name="calendar" size={13} />{formatDateTime(mate.activityTime)}</span><span><AppIcon name="location" size={13} />{mate.location || '地点待定'}</span></div><div className="mate-card-footer"><span className="mate-card-people"><AppIcon name="users" size={14} />{mate.currentParticipants} 人想去{mate.distanceKm != null ? <em>{mate.distanceKm.toFixed(1)} km</em> : null}</span>{isOwner ? <span className="mate-card-note">我发布的</span> : mate.hasJoined ? <span className="mate-card-note">{joinStatus(mate.myApplicationStatus)}</span> : mate.status === 1 && !isEnded ? <button className="mate-join-btn" onClick={(event) => { event.stopPropagation(); onJoin(mate.id); }}>{mate.isFull ? '候补' : '加入'}</button> : <span className="mate-card-note">已结束</span>}</div></div></article>;
}
function formatDateTime(dateStr: string) { if (!dateStr) return '时间待定'; const date = new Date(dateStr); return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`; }
function joinStatus(status?: number | null) { return status === 4 ? '候补中' : status === 0 ? '待审核' : status === 1 ? '已加入' : status === 2 ? '已拒绝' : '已退出'; }
