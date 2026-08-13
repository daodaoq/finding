import type { Post } from '../types/post';
import { formatRelativeTime } from '../utils/format';
import { showToast } from './Toast';
import AppIcon from './AppIcon';
import { usePostShare } from '../hooks/usePostShare';
import ShareCardModal from './ShareCardModal';
import { APP_CONFIG } from '../utils/config';
import './PostCard.css';

interface Props { post: Post; onLike: (id: number) => void; onClick: (id: number) => void; canManage?: boolean; onEdit?: (id: number) => void; onDelete?: (id: number) => void; onFavorite?: (id: number) => void; }

/** 渲染正文中的 @提及(高亮) */
function renderContent(text: string) {
  return text.split(/(@[\p{L}\p{N}_-]{1,20})/u).map((part, i) =>
    part.startsWith('@') ? <span key={i} className="post-mention">{part}</span> : part,
  );
}

export default function PostCard({ post, onLike, onClick, canManage, onEdit, onDelete, onFavorite }: Props) {
  const { preview, setPreview, share } = usePostShare();
  const handleShare = (event: React.MouseEvent) => {
    event.stopPropagation();
    if (!APP_CONFIG.SHARE_CARD_LAUNCHED) {
      showToast('功能即将开放');
      return;
    }
    share(post).catch(() => {});
  };
  const initial = (post.author?.nickname || '匿').slice(0, 1);
  return (<>
    <article className="post-card" onClick={() => onClick(post.id)}>
    <header className="post-header">
      <div className="post-author">
        <div className="post-avatar">{post.author?.avatar ? <img src={post.author.avatar} alt="" /> : <span>{initial}</span>}</div>
        <div className="post-author-info">
          <div className="post-nickname-row">
            <span className="post-nickname">{post.author?.nickname || '匿名用户'}</span>
            {post.isTop === 1 && <span className="post-badge post-badge-top">置顶</span>}
            {post.isHot === 1 && <span className="post-badge post-badge-hot">精华</span>}
            {post.visibility === 1 && <span className="post-badge post-badge-visibility">仅好友</span>}
            {post.visibility === 2 && <span className="post-badge post-badge-visibility">仅自己</span>}
          </div>
          <span className="post-time">{formatRelativeTime(post.createdAt)}{post.location ? ` · ${post.location}` : ''}</span>
        </div>
      </div>
      {canManage && <div className="post-manage" onClick={(event) => event.stopPropagation()}><button className="post-manage-btn" onClick={() => onEdit?.(post.id)}>编辑</button><button className="post-manage-btn danger" onClick={() => onDelete?.(post.id)}>删除</button></div>}
    </header>
    <div className="post-body">{renderContent(post.content)}</div>
    {post.categoryDesc && (
      <div className="post-category-line">
        <AppIcon name="book" size={14} />
        <span>{post.categoryDesc}</span>
      </div>
    )}
    {(post.tags && post.tags.length > 0) && (
      <div className="post-meta-tags">
        {post.tags.map((t) => <span key={t} className="post-tag">#{t}</span>)}
      </div>
    )}
    {post.images?.length ? <div className={`post-images images-${Math.min(post.images.length, 3)}`}>{post.images.slice(0, 3).map((url, index) => <img key={index} src={url} alt="" loading="lazy" />)}</div> : null}
    <footer className="post-footer"><span><AppIcon name="eye" size={15} />{post.viewCount}</span><button className={`like-btn ${post.isLiked ? 'liked' : ''}`} onClick={(event) => { event.stopPropagation(); onLike(post.id); }}><AppIcon name="heart" size={15} />{post.likeCount}</button><span><AppIcon name="message" size={15} />{post.commentCount}</span>{onFavorite && <button className={`fav-btn ${post.isFavorited ? 'favored' : ''}`} onClick={(event) => { event.stopPropagation(); onFavorite(post.id); }} title={post.isFavorited ? '取消收藏' : '收藏'}><AppIcon name="star" size={15} /></button>}<button className="share-btn" onClick={handleShare} title="分享"><AppIcon name="share" size={15} /></button></footer>
  </article>
  {preview && <ShareCardModal visible image={preview} onClose={() => setPreview(null)} />}
  </>);
}
