import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postApi } from '../../../api/post';
import PostCard from '../../../components/PostCard';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import { showToast } from '../../../components/Toast';
import type { Post } from '../../../types/post';
import '../subpage.css';

export default function MyFavoritesPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => { loadFavorites(); }, []);

  const loadFavorites = async () => {
    try {
      const res = await postApi.myFavorites(1, 50);
      setPosts(res.data.data.records);
    } catch { showToast('加载失败'); }
    finally { setLoading(false); }
  };

  const handleFavorite = async (id: number) => {
    try {
      await postApi.favorite(id);
      // 取消收藏后从列表移除
      setPosts((prev) => prev.filter((p) => p.id !== id));
    } catch { showToast('操作失败'); }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>我的收藏</h2>
      </div>
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}
        {!loading && posts.map((p) => (
          <PostCard key={p.id} post={p} onLike={() => {}} onFavorite={handleFavorite} onClick={(id) => navigate(`/square/post/${id}`)} />
        ))}
        {!loading && posts.length === 0 && <EmptyState message="还没有收藏过动态" />}
      </div>
    </div>
  );
}
