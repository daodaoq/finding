import PostCard from '../../../components/PostCard';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import AppIcon from '../../../components/AppIcon';
import type { Post } from '../../../types/post';
// 共享信息流样式（含 .no-more 分页尾部）
import '../../../components/Feed.css';

interface Props {
  posts: Post[];
  loading: boolean;
  hasMore: boolean;
  /** 游客限制：未登录且达到阈值时展示引导登录横幅 */
  showGuestLimit: boolean;
  guestMaxPosts: number;
  onLike: (id: number) => void;
  onFavorite: (id: number) => void;
  onOpen: (id: number) => void;
  onGuestLimitClick: () => void;
}

/** 首页动态信息流（热门/最新/关注） */
export default function PostFeed({
  posts,
  loading,
  hasMore,
  showGuestLimit,
  guestMaxPosts,
  onLike,
  onFavorite,
  onOpen,
  onGuestLimitClick,
}: Props) {
  return (
    <div className="home-post-list">
      {posts.map((post) => (
        <PostCard key={post.id} post={post} onLike={onLike} onFavorite={onFavorite} onClick={onOpen} />
      ))}
      {loading && <LoadingSkeleton />}
      {!loading && posts.length === 0 && <EmptyState message="暂无动态" />}

      {/* 游客限制提示 */}
      {showGuestLimit && posts.length >= guestMaxPosts && hasMore && !loading && (
        <div className="guest-limit-banner" onClick={onGuestLimitClick}>
          <span><AppIcon name="lock" size={20} /></span>
          <div>
            <p className="guest-limit-title">登录查看更多动态</p>
            <p className="guest-limit-sub">登录后即可无限制浏览全部内容</p>
          </div>
          <span className="guest-limit-arrow">›</span>
        </div>
      )}

      {!hasMore && posts.length > 0 && <p className="no-more">— 没有更多了 —</p>}
    </div>
  );
}