import type { Post } from '../types/post';
import { formatRelativeTime } from '../utils/format';
import './PostCard.css';

interface Props {
  post: Post;
  onLike: (id: number) => void;
  onClick: (id: number) => void;
  /** 当前用户是否可管理该动态(作者本人),显示编辑/删除 */
  canManage?: boolean;
  onEdit?: (id: number) => void;
  onDelete?: (id: number) => void;
}

export default function PostCard({ post, onLike, onClick, canManage, onEdit, onDelete }: Props) {
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
            <span className="post-time">{formatRelativeTime(post.createdAt)}</span>
          </div>
        </div>
        <div className="post-header-right">
          {canManage && (
            <div className="post-manage" onClick={(e) => e.stopPropagation()}>
              <button className="post-manage-btn" onClick={() => onEdit?.(post.id)}>编辑</button>
              <button className="post-manage-btn danger" onClick={() => onDelete?.(post.id)}>删除</button>
            </div>
          )}
          {post.location && <span className="post-location">📍 {post.location}</span>}
        </div>
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
