import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mateApi } from '../../api/mate';
import MateCard from '../../components/MateCard';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import LoginModal from '../../components/LoginModal';
import AppIcon, { type AppIconName } from '../../components/AppIcon';
import { showToast } from '../../components/Toast';
import { useInfiniteList } from '../../hooks/useInfiniteList';
import { useGeolocation } from '../../hooks/useGeolocation';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { MATE_CATEGORIES } from '../../utils/constants';
import type { Mate } from '../../types/mate';
import './index.css';

const CATEGORY_ICONS: AppIconName[] = ['location', 'users', 'heart', 'book', 'pen', 'target', 'message', 'calendar'];

export default function MatePage() {
  const [category, setCategory] = useState('');
  const [anonymousOnly, setAnonymousOnly] = useState(false);
  const [city, setCity] = useState('');
  const [daysAhead, setDaysAhead] = useState(0);
  const [availableOnly, setAvailableOnly] = useState(false);
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const navigate = useNavigate();
  const { lat, lng } = useGeolocation(true);
  const { showLogin, requireLogin, handleLoginSuccess, handleClose } = useRequireLogin();

  const { items: mates, loading, hasMore, setItems: setMates, onScroll } = useInfiniteList<Mate, [string, number?, number?]>({
    fetcher: async (page, selectedCategory, latitude, longitude) => {
      const params: Record<string, unknown> = { page, size: 10 };
      if (selectedCategory) params.category = selectedCategory;
      if (keyword) params.keyword = keyword;
      if (anonymousOnly) params.anonymousOnly = true;
      if (city.trim()) params.city = city.trim();
      if (daysAhead) params.daysAhead = daysAhead;
      if (availableOnly) params.availableOnly = true;
      if (latitude != null && longitude != null) { params.latitude = latitude; params.longitude = longitude; params.radiusKm = 20; }
      return (await mateApi.list(params)).data.data;
    },
    args: [category, lat, lng],
    deps: [category, keyword, lat, lng, anonymousOnly, city, daysAhead, availableOnly],
    onError: () => showToast('加载失败'),
  });

  const submitSearch = useCallback(() => setKeyword(keywordInput.trim()), [keywordInput]);
  const handleJoin = (id: number) => {
    // 未登录先弹登录框,登录成功后再执行报名
    requireLogin(async () => {
      const message = window.prompt('可以给发起人留一句话（选填，最多 500 字）', '') ?? undefined;
      if (message !== undefined && message.length > 500) { showToast('留言不能超过 500 字'); return; }
      try {
        await mateApi.join(id, message);
        setMates((previous) => previous.filter((mate) => mate.id !== id));
        showToast('申请已提交');
      } catch (error: any) { showToast(error?.message || '操作失败'); }
    });
  };

  return (
    <div className="mate-page" onScroll={onScroll}>
      <header className="mate-header">
        <div className="mate-header-row"><h1>找搭子</h1><button className="mate-create-btn" onClick={() => navigate('/create-mate')}>发布活动</button></div>
        <div className="mate-search">
          <AppIcon name="search" size={18} />
          <input type="search" value={keywordInput} placeholder="搜索活动、地点或关键词" onChange={(event) => setKeywordInput(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && submitSearch()} />
          {keywordInput ? <button className="mate-search-clear" onClick={() => { setKeywordInput(''); setKeyword(''); }} aria-label="清除搜索"><AppIcon name="close" size={16} /></button> : null}
        </div>
      </header>
      <section className="mate-category-section" aria-label="活动分类"><div className="mate-category-grid">
        {MATE_CATEGORIES.slice(0, 8).map((item, index) => <button key={item.code} className={`mate-category-item ${category === item.code ? 'is-active' : ''}`} onClick={() => setCategory(category === item.code ? '' : item.code)}><span className={`mate-category-icon mate-category-icon--${index}`}><AppIcon name={CATEGORY_ICONS[index]} size={22} strokeWidth={2} /></span><span>{item.name.replace('搭子', '')}</span></button>)}
        <button className="mate-category-item" onClick={() => setCategory('')}><span className="mate-category-icon mate-category-icon--more"><AppIcon name="inbox" size={21} /></span><span>全部</span></button>
      </div></section>
      <section className="mate-filter-bar" aria-label="活动筛选">
        <label><AppIcon name="location" size={15} /><input value={city} onChange={(event) => setCity(event.target.value)} placeholder="地点" /></label>
        <label><AppIcon name="calendar" size={15} /><select value={daysAhead} onChange={(event) => setDaysAhead(Number(event.target.value))}><option value={0}>时间</option><option value={1}>今天</option><option value={7}>7天内</option><option value={30}>30天内</option></select></label>
        <button className={availableOnly ? 'is-selected' : ''} onClick={() => setAvailableOnly((value) => !value)}>有空位</button><button className={anonymousOnly ? 'is-selected' : ''} onClick={() => setAnonymousOnly((value) => !value)}>匿名</button>
      </section>
      <section className="mate-list-section"><div className="mate-list-header"><span>推荐活动</span><button className="sort-hint" onClick={submitSearch}>按距离 · 时间 <AppIcon name="right" size={14} /></button></div>
        {mates.map((mate) => <MateCard key={mate.id} mate={mate} onJoin={handleJoin} onClick={() => navigate(`/mate/${mate.id}`)} />)}
        {loading ? <LoadingSkeleton /> : null}
        {!loading && mates.length === 0 ? <EmptyState message="暂无搭子邀约" /> : null}
        {!hasMore && mates.length > 0 ? <p className="no-more">— 没有更多活动了 —</p> : null}
      </section>

      {/* 登录弹窗(未登录报名时弹出) */}
      <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />
    </div>
  );
}
