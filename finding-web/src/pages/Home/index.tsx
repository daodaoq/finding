import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postApi } from '../../api/post';
import { mateApi } from '../../api/mate';
import { useInfiniteList } from '../../hooks/useInfiniteList';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { useGeolocation } from '../../hooks/useGeolocation';
import LoginModal from '../../components/LoginModal';
import { showToast } from '../../components/Toast';
import { APP_CONFIG } from '../../utils/config';
import type { Post } from '../../types/post';
import type { Mate } from '../../types/mate';
import PostFeed from './components/PostFeed';
import MateFeed from './components/MateFeed';
import AppIcon from '../../components/AppIcon';
import './index.css';

const HOME_TABS = [
  { key: 'latest', label: '最新' },
  { key: 'hot', label: '热门' },
  { key: 'following', label: '关注' },
  { key: 'mate', label: '搭子' },
] as const;

const SORT_OPTIONS = [
  { key: 'views', label: '浏览量最高' },
  { key: 'likes', label: '点赞率最高' },
  { key: 'recommended', label: '值得推荐' },
] as const;

/** 游客最多浏览的帖子数 */
const GUEST_MAX_POSTS = 5;

export default function HomePage() {
  const [activeTab, setActiveTab] = useState('latest');
  const [sortBy, setSortBy] = useState('recommended');
  // 搭子 Tab 相关状态
  const [mateCategory, setMateCategory] = useState('');
  const [mateSortBy, setMateSortBy] = useState('time');

  const navigate = useNavigate();
  const { showLogin, requireLogin, handleLoginSuccess, handleClose, isLoggedIn } = useRequireLogin();
  // 搭子 Tab 下获取浏览器定位(用于距离)
  const { lat, lng } = useGeolocation(activeTab === 'mate');

  const isPostTab = activeTab !== 'mate';

  // 帖子列表：切换 Tab / 排序时重置（搭子 Tab 下不请求）
  const postList = useInfiniteList<Post, [string, string?]>({
    fetcher: async (p, tab, sort) => {
      if (tab === 'mate') return { records: [], hasMore: false };
      const res = await postApi.list({
        tab,
        page: p,
        size: 10,
        sortBy: tab === 'hot' ? sort : undefined,
      } as Record<string, unknown>);
      return res.data.data;
    },
    args: [activeTab, isPostTab ? sortBy : undefined],
    deps: [isPostTab ? activeTab : null, sortBy],
    onError: () => showToast('加载失败'),
  });

  // 搭子列表：切换 Tab / 分类 / 排序时重置
  const mateList = useInfiniteList<Mate, [string, string, number?, number?]>({
    fetcher: async (p, cat, sort, la, ln) => {
      const params: Record<string, unknown> = { page: p, size: 10, sortBy: sort };
      if (cat) params.category = cat;
      if (la != null && ln != null) { params.lat = la; params.lng = ln; }
      const res = await mateApi.list(params);
      return res.data.data;
    },
    args: [mateCategory, mateSortBy, lat, lng],
    deps: [isPostTab ? null : activeTab, mateCategory, mateSortBy, lat, lng],
    onError: () => showToast('加载失败'),
  });

  const handleLike = (id: number) => {
    requireLogin(async () => {
      try {
        await postApi.like(id);
        postList.setItems((prev) => prev.map((p) =>
          p.id === id ? { ...p, isLiked: !p.isLiked, likeCount: p.isLiked ? p.likeCount - 1 : p.likeCount + 1 } : p));
      } catch { showToast('操作失败'); }
    });
  };

  const handleJoinMate = (id: number) => {
    requireLogin(async () => {
      try {
        await mateApi.join(id);
        mateList.setItems((prev) => prev.map((m) =>
          m.id === id ? { ...m, hasJoined: true, currentParticipants: m.currentParticipants + 1 } : m));
      } catch { showToast('操作失败'); }
    });
  };

  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    if (activeTab === 'mate') {
      mateList.onScroll(e);
      return;
    }
    // 游客限制：未登录最多浏览 GUEST_MAX_POSTS 条
    if (!isLoggedIn && postList.items.length >= GUEST_MAX_POSTS) {
      requireLogin(() => {});
      return;
    }
    postList.onScroll(e);
  };

  return (
    <div className="home-page" onScroll={handleScroll}>
      {/* 顶部：校名 + 搜索框 */}
      <div className="home-top">
        <span className="home-school">{APP_CONFIG.SCHOOL_NAME}</span>
        <div className="home-search-wrapper" onClick={() => navigate('/search')}>
          <AppIcon name="search" className="home-search-icon" size={17} />
          <input className="home-search-input" type="text" placeholder="搜索用户、动态、搭子..." readOnly />
        </div>
      </div>

      {/* Tab 切换 */}
      <div className="home-tabs">
        {HOME_TABS.map((tab) => (
          <button key={tab.key}
            className={`home-tab ${activeTab === tab.key ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.key)}>
            {tab.label}
          </button>
        ))}
      </div>

      {/* 帖子 Tab */}
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

          <PostFeed
            posts={postList.items}
            loading={postList.loading}
            hasMore={postList.hasMore}
            showGuestLimit={!isLoggedIn}
            guestMaxPosts={GUEST_MAX_POSTS}
            onLike={handleLike}
            onOpen={(id) => navigate(`/square/post/${id}`)}
            onGuestLimitClick={() => requireLogin(() => {})}
          />
        </>
      )}

      {/* 搭子 Tab */}
      {!isPostTab && (
        <MateFeed
          mates={mateList.items}
          loading={mateList.loading}
          hasMore={mateList.hasMore}
          category={mateCategory}
          sortBy={mateSortBy}
          onCategoryChange={(code) => setMateCategory(mateCategory === code ? '' : code)}
          onSortChange={setMateSortBy}
          onJoin={handleJoinMate}
          onOpen={(id) => navigate(`/mate/${id}`)}
        />
      )}

      {/* 登录弹窗 */}
      <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />
    </div>
  );
}
