import type { Post } from '../types/post';
import { formatRelativeTime } from '../utils/format';
import { APP_CONFIG } from '../utils/config';
import './PostShareCard.css';

/**
 * 帖子分享卡片快照:固定 420px 宽度、3:4 左右比例的"帖子快照"。
 * 渲染于屏幕外容器,由 html-to-image 截图为 PNG。
 * 样式尽量自包含(字体用系统字体栈),保证快照保真、不依赖页面全局样式。
 */
export default function PostShareCard({ post }: { post: Post }) {
  const cover = post.images?.[0];
  const initial = (post.author?.nickname || '匿').slice(0, 1);
  return (
    <div className="psc-card">
      <div className="psc-brand">
        <span className="psc-brand-name">Finding</span>
        <span className="psc-school">{APP_CONFIG.SCHOOL_NAME}</span>
      </div>

      {cover ? <img className="psc-cover" src={cover} alt="" crossOrigin="anonymous" /> : null}

      <p className="psc-content">{post.content}</p>

      <div className="psc-divider" />

      <div className="psc-author">
        <div className="psc-avatar">
          {post.author?.avatar ? <img src={post.author.avatar} alt="" crossOrigin="anonymous" /> : <span>{initial}</span>}
        </div>
        <div className="psc-author-info">
          <span className="psc-nickname">{post.author?.nickname || '匿名用户'}</span>
          <span className="psc-time">{formatRelativeTime(post.createdAt)}{post.location ? ` · ${post.location}` : ''}</span>
        </div>
      </div>

      <div className="psc-stats">
        <span className="psc-stat">♥ {post.likeCount}</span>
        <span className="psc-stat">💬 {post.commentCount}</span>
        <span className="psc-stat">👁 {post.viewCount}</span>
      </div>

      <div className="psc-foot">来 Finding 看更多校园故事</div>
    </div>
  );
}
