import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postApi } from '../../api/post';
import type { Post } from '../../types/post';
import RailCard from './RailCard';
import './rail.css';

/** 热榜 Top5:热门帖子按浏览量排序(复用首页热门接口,编号即排名)。 */
export default function RailHotPosts() {
  const navigate = useNavigate();
  const [posts, setPosts] = useState<Post[] | null>(null);

  useEffect(() => {
    let alive = true;
    postApi.list({ tab: 'hot', page: 1, size: 5, sortBy: 'views' } as Record<string, unknown>)
      .then((res) => { if (alive) setPosts(res.data.data?.records ?? []); })
      .catch(() => { if (alive) setPosts([]); });
    return () => { alive = false; };
  }, []);

  return (
    <RailCard title="热榜">
      {posts === null && <p className="rail-empty">加载中...</p>}
      {posts !== null && posts.length === 0 && <p className="rail-empty">暂无热门内容</p>}
      {posts !== null && posts.map((p, i) => (
        <button key={p.id} className="rail-hot-row" onClick={() => navigate(`/square/post/${p.id}`)}>
          <span className={`rail-rank ${i < 3 ? 'is-top' : ''}`}>{i + 1}</span>
          <span className="rail-hot-title">{p.content || '(无文字)'}</span>
          <span className="rail-hot-meta">{p.viewCount}</span>
        </button>
      ))}
    </RailCard>
  );
}
