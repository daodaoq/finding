import type { HomeFeedUser } from '../types/message';
import './UserCard.css';

interface Props {
  user: HomeFeedUser;
  onLike: (userId: number) => void;
}

export default function UserCard({ user, onLike }: Props) {
  return (
    <div className="user-card">
      <div className="user-card-header">
        <div className="user-card-avatar">
          {user.avatar ? <img src={user.avatar} alt="" /> : <span>👤</span>}
        </div>
        <div className="user-card-info">
          <div className="user-card-name">
            {user.nickname}
            <span className={`gender-tag ${user.gender === 1 ? 'male' : 'female'}`}>
              {user.gender === 1 ? '♂' : '♀'}
            </span>
          </div>
          <div className="user-card-meta">
            {user.school && <span>🎓 {user.school}</span>}
            {user.city && <span>📍 {user.city}</span>}
          </div>
        </div>
        <button className="more-btn">⋯</button>
      </div>
      {user.signature && <div className="user-card-bio">{user.signature}</div>}
      <div className="user-card-footer">
        <span className="time-text">{formatRelativeTime(user.lastLoginAt)}</span>
        <button
          className={`heart-btn ${user.isLiked ? 'liked' : ''}`}
          onClick={(e) => { e.stopPropagation(); onLike(user.userId); }}
        >
          {user.isLiked ? '❤️' : '🤍'}
        </button>
      </div>
    </div>
  );
}

function formatRelativeTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 60000) return '刚刚在线';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前在线`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前在线`;
  return `${Math.floor(diff / 86400000)}天前在线`;
}
