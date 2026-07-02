import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postApi } from '../../../api/post';
import PostCard from '../../../components/PostCard';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import { showToast } from '../../../components/Toast';
import type { Post } from '../../../types/post';
import '../subpage.css';

export default function MyPostsPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const navigate = useNavigate();

  useEffect(() => { loadPosts(1); }, []);

  const loadPosts = async (p: number) => {
    setLoading(true);
    try {
      const res = await postApi.myPosts(p, 10);
      const data = res.data.data;
      if (p === 1) setPosts(data.records);
      else setPosts((prev) => [...prev, ...data.records]);
      setHasMore(data.hasMore);
      setPage(p);
    } catch { showToast('加载失败'); }
    finally { setLoading(false); }
  };

  const handleLike = async (id: number) => {
    try { await postApi.like(id);
      setPosts(prev => prev.map(p => p.id === id ? { ...p, isLiked: !p.isLiked, likeCount: p.isLiked ? p.likeCount - 1 : p.likeCount + 1 } : p));
    } catch { showToast('操作失败'); }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>我的动态</h2>
      </div>
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}
        {!loading && posts.map(p => <PostCard key={p.id} post={p} onLike={handleLike} onClick={id => navigate(`/square/post/${id}`)} />)}
        {!loading && posts.length === 0 && <EmptyState message="还没有发布过动态" />}
        {hasMore && posts.length > 0 && (
          <button className="load-more-btn" onClick={() => loadPosts(page + 1)} disabled={loading}>
            {loading ? '加载中...' : '加载更多'}
          </button>
        )}
      </div>
    </div>
  );
}
