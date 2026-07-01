import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postApi } from '../../api/post';
import SearchBar from '../../components/SearchBar';
import PostCard from '../../components/PostCard';
import FloatingActionButton from '../../components/FloatingActionButton';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { SQUARE_TABS } from '../../utils/constants';
import type { Post } from '../../types/post';
import './index.css';

export default function SquarePage() {
  const [activeTab, setActiveTab] = useState('hot');
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    setPosts([]);
    setPage(1);
    setHasMore(true);
    loadPosts(1, activeTab);
  }, [activeTab]);

  const loadPosts = async (p: number, tab: string) => {
    setLoading(true);
    try {
      const res = await postApi.list({ tab, page: p, size: 10 });
      const data = res.data.data;
      if (p === 1) setPosts(data.records);
      else setPosts((prev) => [...prev, ...data.records]);
      setHasMore(data.hasMore);
      setPage(p);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const handleLike = async (id: number) => {
    try {
      await postApi.like(id);
      setPosts((prev) => prev.map((p) =>
        p.id === id ? { ...p, isLiked: !p.isLiked, likeCount: p.isLiked ? p.likeCount - 1 : p.likeCount + 1 } : p));
    } catch { /* ignore */ }
  };

  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const bottom = e.currentTarget.scrollHeight - e.currentTarget.scrollTop <=
                   e.currentTarget.clientHeight + 100;
    if (bottom && hasMore && !loading) loadPosts(page + 1, activeTab);
  };

  return (
    <div className="square-page" onScroll={handleScroll}>
      <SearchBar placeholder="搜搭子" />

      {/* Tabs */}
      <div className="square-tabs">
        {SQUARE_TABS.map((tab) => (
          <button
            key={tab.key}
            className={`square-tab ${activeTab === tab.key ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Post list */}
      <div className="post-list">
        {posts.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            onLike={handleLike}
            onClick={(id) => navigate(`/square/post/${id}`)}
          />
        ))}
        {loading && <LoadingSkeleton />}
        {!loading && posts.length === 0 && <EmptyState message="暂无动态" />}
        {!hasMore && posts.length > 0 && (
          <p className="no-more">— 没有更多了 —</p>
        )}
      </div>

      <FloatingActionButton onClick={() => navigate('/square/create')} />
    </div>
  );
}
