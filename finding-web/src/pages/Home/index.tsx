import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postApi } from '../../api/post';
import { mateApi } from '../../api/mate';
import PostCard from '../../components/PostCard';
import MateCard from '../../components/MateCard';
import LoginModal from '../../components/LoginModal';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { MATE_CATEGORIES } from '../../utils/constants';
import type { Post } from '../../types/post';
import type { Mate } from '../../types/mate';
import './index.css';

const HOME_TABS = [
  { key: 'hot', label: '热门' },
  { key: 'latest', label: '最新' },
  { key: 'following', label: '关注' },
  { key: 'mate', label: '搭子' },
] as const;

const SORT_OPTIONS = [
  { key: 'views', label: '浏览量最高' },
  { key: 'likes', label: '点赞率最高' },
  { key: 'recommended', label: '值得推荐' },
] as const;

const MATE_SORT_OPTIONS = [
  { key: 'time', label: '时间最近' },
  { key: 'distance', label: '距离最近' },
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

  // 搭子 Tab 相关状态
  const [mateCategory, setMateCategory] = useState('');
  const [mateSortBy, setMateSortBy] = useState('time');
  const [mates, setMates] = useState<Mate[]>([]);

  const navigate = useNavigate();
  const { showLogin, requireLogin, handleLoginSuccess, handleClose, isLoggedIn } = useRequireLogin();

  const isPostTab = activeTab !== 'mate';

  // 帖子 Tab: 切换时重置
  useEffect(() => {
    if (activeTab === 'mate') return;
    setPosts([]);
    setPage(1);
    setHasMore(true);
    loadPosts(1, activeTab, sortBy);
  }, [activeTab, sortBy]);

  // 搭子 Tab: 切换分类或排序时重置
  useEffect(() => {
    if (activeTab !== 'mate') return;
    setMates([]);
    setPage(1);
    setHasMore(true);
    loadMates(1, mateCategory, mateSortBy);
  }, [activeTab, mateCategory, mateSortBy]);

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

  const loadMates = async (p: number, cat: string, sort: string) => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page: p, size: 10 };
      if (cat) params.category = cat;
      if (sort === 'time') params.sortBy = 'time';
      else params.sortBy = 'distance';

      const res = await mateApi.list(params);
      const data = res.data.data;
      if (p === 1) setMates(data.records);
      else setMates((prev) => [...prev, ...data.records]);
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

  const handleJoinMate = (id: number) => {
    requireLogin(async () => {
      try {
        await mateApi.join(id);
        setMates((prev) => prev.map((m) =>
          m.id === id ? { ...m, hasJoined: true, currentParticipants: m.currentParticipants + 1 } : m));
      } catch { /* ignore */ }
    });
  };

  const handleTabChange = (key: string) => {
    setActiveTab(key);
    setPage(1);
    setHasMore(true);
  };

  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const bottom = e.currentTarget.scrollHeight - e.currentTarget.scrollTop <=
                   e.currentTarget.clientHeight + 100;
    if (!bottom || !hasMore || loading) return;

    // 游客限制：最多看 GUEST_MAX_POSTS 条
    if (!isLoggedIn && isPostTab && posts.length >= GUEST_MAX_POSTS) {
      requireLogin(() => {});
      return;
    }

    if (isPostTab) {
      loadPosts(page + 1, activeTab, sortBy);
    } else {
      loadMates(page + 1, mateCategory, mateSortBy);
    }
  };

  return (
    <div className="home-page" onScroll={handleScroll}>
      {/* 顶部：学校名 + 搜索框 */}
      <div className="home-top">
        <span className="home-school">山东理工大学</span>
        <div className="home-search-wrapper" onClick={() => navigate('/search')}>
          <span className="home-search-icon">🔍</span>
          <input className="home-search-input" type="text" placeholder="搜索用户、动态、搭子..." readOnly />
        </div>
      </div>

      {/* Tab 切换 */}
      <div className="home-tabs">
        {HOME_TABS.map((tab) => (
          <button key={tab.key}
            className={`home-tab ${activeTab === tab.key ? 'active' : ''}`}
            onClick={() => handleTabChange(tab.key)}>
            {tab.label}
          </button>
        ))}
      </div>

      {/* 帖子 Tab 内容 */}
      {isPostTab && (
        <>
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
        </>
      )}

      {/* 搭子 Tab 内容 */}
      {!isPostTab && (
        <>
          {/* 分类筛选网格 */}
          <div className="square-categories">
            <div className="category-grid">
              {MATE_CATEGORIES.map((cat) => (
                <button
                  key={cat.code}
                  className={`category-cell ${mateCategory === cat.code ? 'active' : ''}`}
                  onClick={() => setMateCategory(mateCategory === cat.code ? '' : cat.code)}
                >
                  <span className="category-cell-icon">{cat.icon}</span>
                  <span className="category-cell-name">{cat.name}</span>
                </button>
              ))}
            </div>
          </div>

          {/* 排序选项 */}
          <div className="square-sort-bar">
            <span className="sort-label">
              {mateCategory ? MATE_CATEGORIES.find(c => c.code === mateCategory)?.name : '全部搭子'}
            </span>
            <div className="sort-options">
              {MATE_SORT_OPTIONS.map((opt) => (
                <button
                  key={opt.key}
                  className={`sort-chip ${mateSortBy === opt.key ? 'active' : ''}`}
                  onClick={() => setMateSortBy(opt.key)}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* 搭子列表 */}
          <div className="square-mate-list">
            {mates.map((mate) => (
              <MateCard
                key={mate.id}
                mate={mate}
                onJoin={handleJoinMate}
                onClick={(id) => navigate(`/mate/${id}`)}
              />
            ))}
            {loading && <LoadingSkeleton />}
            {!loading && mates.length === 0 && (
              <EmptyState icon="🔍" message={
                mateCategory
                  ? `暂无"${MATE_CATEGORIES.find(c => c.code === mateCategory)?.name}"邀约`
                  : '暂无搭子邀约'
              } />
            )}
            {!hasMore && mates.length > 0 && <p className="no-more">— 没有更多了 —</p>}
          </div>
        </>
      )}

      {/* 登录弹窗 */}
      <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />
    </div>
  );
}
