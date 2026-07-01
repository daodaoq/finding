import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postApi } from '../../../api/post';
import PostCard from '../../../components/PostCard';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import type { Post } from '../../../types/post';
import '../subpage.css';

export default function MyLikesPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => { loadLikes(); }, []);

  const loadLikes = async () => {
    try {
      const res = await postApi.myLikes(1, 50);
      setPosts(res.data.data.records);
    } catch { /* */ }
    finally { setLoading(false); }
  };

  const handleLike = async (id: number) => {
    try { await postApi.like(id);
      // 取消点赞后从列表中移除
      setPosts(prev => {
        const post = prev.find(p => p.id === id);
        if (post?.isLiked) {
          return prev.filter(p => p.id !== id);
        }
        return prev.map(p => p.id === id ? { ...p, isLiked: true, likeCount: p.likeCount + 1 } : p);
      });
    } catch { /* */ }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>我的点赞</h2>
      </div>
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}
        {!loading && posts.map(p => <PostCard key={p.id} post={p} onLike={handleLike} onClick={id => navigate(`/square/post/${id}`)} />)}
        {!loading && posts.length === 0 && <EmptyState message="还没有点赞过动态" />}
      </div>
    </div>
  );
}
