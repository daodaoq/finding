import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postApi } from '../../api/post';
import PostCard from '../../components/PostCard';
import LoginModal from '../../components/LoginModal';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { SQUARE_TABS } from '../../utils/constants';
import type { Post } from '../../types/post';
import './index.css';

const SORT_OPTIONS = [
  { key: 'views', label: '浏览量最高' },
  { key: 'likes', label: '点赞率最高' },
  { key: 'recommended', label: '值得推荐' },
] as const;

/** 游客最多浏览的帖子数 */
const GUEST_MAX_POSTS = 5;

export default function HomePage() {
  const [activeTab, setActiveTab] = useState('hot');
  const [sortBy, setSortBy] = useState('recommended');
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const navigate = useNavigate();
  const { showLogin, requireLogin, handleLoginSuccess, handleClose, isLoggedIn } = useRequireLogin();

  useEffect(() => {
    setPosts([]);
    setPage(1);
    setHasMore(true);
    loadPosts(1, activeTab, sortBy);
  }, [activeTab, sortBy]);

  const loadPosts = async (p: number, tab: string, sort?: string) => {
    setLoading(true);
    try {
      const res = await postApi.list({
        tab,
        page: p,
        size: 10,
        sortBy: tab === 'hot' ? sort : undefined,
      } as Record<string, unknown>);
      const data = res.data.data;
      if (p === 1) setPosts(data.records);
      else setPosts((prev) => [...prev, ...data.records]);
      setHasMore(data.hasMore);
      setPage(p);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const handleLike = (id: number) => {
    requireLogin(async () => {
      try {
        await postApi.like(id);
        setPosts((prev) => prev.map((p) =>
          p.id === id ? { ...p, isLiked: !p.isLiked, likeCount: p.isLiked ? p.likeCount - 1 : p.likeCount + 1 } : p));
      } catch { /* ignore */ }
    });
  };

  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const bottom = e.currentTarget.scrollHeight - e.currentTarget.scrollTop <=
                   e.currentTarget.clientHeight + 100;
    if (!bottom || !hasMore || loading) return;

    // 游客限制：最多看 GUEST_MAX_POSTS 条
    if (!isLoggedIn && posts.length >= GUEST_MAX_POSTS) {
      requireLogin(() => {});
      return;
    }

    loadPosts(page + 1, activeTab, sortBy);
  };

  return (
    <div className="home-page" onScroll={handleScroll}>
      {/* 顶部：学校名 + 搜索框 */}
      <div className="home-top">
        <span className="home-school">山东理工大学</span>
        <div className="home-search-wrapper">
          <span className="home-search-icon">🔍</span>
          <input className="home-search-input" type="text" placeholder="搜搭子..." />
        </div>
      </div>

      {/* Tab 切换 */}
      <div className="home-tabs">
        {SQUARE_TABS.map((tab) => (
          <button key={tab.key}
            className={`home-tab ${activeTab === tab.key ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.key)}>
            {tab.label}
          </button>
        ))}
      </div>

      {/* 热门子排序 */}
      {activeTab === 'hot' && (
        <div className="home-sort-bar">
          {SORT_OPTIONS.map((opt) => (
            <button key={opt.key}
              className={`sort-btn ${sortBy === opt.key ? 'active' : ''}`}
              onClick={() => setSortBy(opt.key)}>
              {opt.label}
            </button>
          ))}
        </div>
      )}

      {/* 动态列表 */}
      <div className="home-post-list">
        {posts.map((post) => (
          <PostCard key={post.id} post={post} onLike={handleLike}
            onClick={(id) => navigate(`/square/post/${id}`)} />
        ))}
        {loading && <LoadingSkeleton />}
        {!loading && posts.length === 0 && <EmptyState message="暂无动态" />}

        {/* 游客限制提示 */}
        {!isLoggedIn && posts.length >= GUEST_MAX_POSTS && hasMore && !loading && (
          <div className="guest-limit-banner" onClick={() => requireLogin(() => {})}>
            <span>🔒</span>
            <div>
              <p className="guest-limit-title">登录查看更多动态</p>
              <p className="guest-limit-sub">登录后即可无限制浏览全部内容</p>
            </div>
            <span className="guest-limit-arrow">›</span>
          </div>
        )}

        {!hasMore && posts.length > 0 && (
          <p className="no-more">— 没有更多了 —</p>
        )}
      </div>

      {/* 登录弹窗 */}
      <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />
    </div>
  );
}
