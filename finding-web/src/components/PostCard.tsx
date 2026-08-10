import type { Post } from '../types/post';
import { formatRelativeTime } from '../utils/format';
import { showToast } from './Toast';
import AppIcon from './AppIcon';
import './PostCard.css';

interface Props { post: Post; onLike: (id: number) => void; onClick: (id: number) => void; canManage?: boolean; onEdit?: (id: number) => void; onDelete?: (id: number) => void; }

export default function PostCard({ post, onLike, onClick, canManage, onEdit, onDelete }: Props) {
  const handleShare = (event: React.MouseEvent) => {
    event.stopPropagation();
    const url = `${window.location.origin}/square/post/${post.id}`;
    if (navigator.share) navigator.share({ title: 'Finding', text: post.content?.slice(0, 30) || '', url }).catch(() => {});
    else if (navigator.clipboard) navigator.clipboard.writeText(url).then(() => showToast('链接已复制')).catch(() => {});
  };
  const initial = (post.author?.nickname || '匿').slice(0, 1);
  return <article className="post-card" onClick={() => onClick(post.id)}>
    <header className="post-header"><div className="post-author"><div className="post-avatar">{post.author?.avatar ? <img src={post.author.avatar} alt="" /> : <span>{initial}</span>}</div><div className="post-author-info"><span className="post-nickname">{post.author?.nickname || '匿名用户'}</span><span className="post-time">{formatRelativeTime(post.createdAt)}{post.location ? ` · ${post.location}` : ''}</span></div></div>
      {canManage && <div className="post-manage" onClick={(event) => event.stopPropagation()}><button className="post-manage-btn" onClick={() => onEdit?.(post.id)}>编辑</button><button className="post-manage-btn danger" onClick={() => onDelete?.(post.id)}>删除</button></div>}
    </header>
    <div className="post-body">{post.content}</div>
    {post.images?.length ? <div className={`post-images images-${Math.min(post.images.length, 3)}`}>{post.images.slice(0, 3).map((url, index) => <img key={index} src={url} alt="" loading="lazy" />)}</div> : null}
    <footer className="post-footer"><span><AppIcon name="eye" size={15} />{post.viewCount}</span><button className={`like-btn ${post.isLiked ? 'liked' : ''}`} onClick={(event) => { event.stopPropagation(); onLike(post.id); }}><AppIcon name="heart" size={15} />{post.likeCount}</button><span><AppIcon name="message" size={15} />{post.commentCount}</span><button className="share-btn" onClick={handleShare} title="分享"><AppIcon name="share" size={15} /></button></footer>
  </article>;
}
