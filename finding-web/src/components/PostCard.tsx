import type { Post } from '../types/post';
import './PostCard.css';

interface Props {
  post: Post;
  onLike: (id: number) => void;
  onClick: (id: number) => void;
}

export default function PostCard({ post, onLike, onClick }: Props) {
  return (
    <div className="post-card" onClick={() => onClick(post.id)}>
      <div className="post-header">
        <div className="post-author">
          <div className="post-avatar">
            {post.author?.avatar ? (
              <img src={post.author.avatar} alt="" />
            ) : (
              <span>👤</span>
            )}
          </div>
          <div className="post-author-info">
            <span className="post-nickname">{post.author?.nickname || '匿名用户'}</span>
            <span className="post-time">{formatTime(post.createdAt)}</span>
          </div>
        </div>
        {post.location && <span className="post-location">📍 {post.location}</span>}
      </div>
      <div className="post-body">{post.content}</div>
      {post.images && post.images.length > 0 && (
        <div className={`post-images images-${Math.min(post.images.length, 3)}`}>
          {post.images.slice(0, 3).map((url, i) => (
            <img key={i} src={url} alt="" loading="lazy" />
          ))}
        </div>
      )}
      <div className="post-footer">
        <span>👁 {post.viewCount}</span>
        <button
          className={`like-btn ${post.isLiked ? 'liked' : ''}`}
          onClick={(e) => { e.stopPropagation(); onLike(post.id); }}
        >
          {post.isLiked ? '❤️' : '🤍'} {post.likeCount}
        </button>
        <span>💬 {post.commentCount}</span>
      </div>
    </div>
  );
}

function formatTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  return d.toLocaleDateString('zh-CN');
}
