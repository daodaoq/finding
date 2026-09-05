import RailAuthorCard from './RailAuthorCard';
import RailRelated from './RailRelated';
import type { Post } from '../../types/post';
import './rail.css';

/** 帖子详情右侧栏组合件(桌面 ≥768px 由 PostDetail 以 useIsDesktop 挂载)。 */
export default function PostRail({ post }: { post: Post }) {
  return (
    <aside className="pd-rail">
      <div className="rail-sticky">
        {post.author && <RailAuthorCard author={post.author} />}
        {post.category && <RailRelated category={post.category} excludeId={post.id} />}
      </div>
    </aside>
  );
}
