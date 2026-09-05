import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postApi } from '../../api/post';
import type { Post } from '../../types/post';
import RailCard from './RailCard';
import './rail.css';

/** 同分类最新帖子(排除当前贴),点击进详情;无内容则整卡不渲染。 */
export default function RailRelated({ category, excludeId }: { category: string; excludeId: number }) {
  const navigate = useNavigate();
  const [posts, setPosts] = useState<Post[] | null>(null);

  useEffect(() => {
    let alive = true;
    postApi.list({ tab: 'latest', category, page: 1, size: 6 })
      .then((res) => {
        if (!alive) return;
        setPosts((res.data.data?.records ?? []).filter((p) => p.id !== excludeId).slice(0, 5));
      })
      .catch(() => { if (alive) setPosts([]); });
    return () => { alive = false; };
  }, [category, excludeId]);

  if (posts !== null && posts.length === 0) return null;
  return (
    <RailCard title="相关动态">
      {posts === null && <p className="rail-empty">加载中...</p>}
      {posts !== null && posts.map((p) => (
        <button key={p.id} className="rail-hot-row" onClick={() => navigate(`/square/post/${p.id}`)}>
          <span className="rail-hot-title">{p.content || '(无文字)'}</span>
          <span className="rail-hot-meta">{p.commentCount}评</span>
        </button>
      ))}
    </RailCard>
  );
}
