import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import request from '../../api/request';
import { showToast } from '../../components/Toast';
import EmptyState from '../../components/EmptyState';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import './index.css';

interface SearchResult {
  users: { records: any[]; total: number };
  posts: { records: any[]; total: number };
  mates: { records: any[]; total: number };
}

const TABS = [
  { key: 'all', label: '全部' },
  { key: 'users', label: '用户' },
  { key: 'posts', label: '动态' },
  { key: 'mates', label: '搭子' },
];

export default function SearchPage() {
  const [keyword, setKeyword] = useState('');
  const [activeTab, setActiveTab] = useState('all');
  const [results, setResults] = useState<SearchResult | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const doSearch = async (kw?: string) => {
    const q = kw || keyword;
    if (!q.trim()) return;
    setLoading(true);
    try {
      const res = await request.get('/search', { params: { keyword: q.trim() } });
      setResults(res.data.data);
    } catch { showToast('搜索失败'); }
    finally { setLoading(false); }
  };

  // 进入页面自动聚焦
  useEffect(() => {
    const input = document.querySelector<HTMLInputElement>('.search-page-input');
    input?.focus();
  }, []);

  const showTab = (tab: string) => {
    if (activeTab === 'all') return true;
    return activeTab === tab;
  };

  const hasAnyResult = results && (
    results.users.total > 0 || results.posts.total > 0 || results.mates.total > 0
  );

  return (
    <div className="search-page">
      {/* 顶部搜索栏 */}
      <div className="search-page-top">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <input
          className="search-page-input"
          type="text"
          placeholder="搜索用户、动态、搭子..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') doSearch(); }}
        />
        <button className="search-page-btn" onClick={() => doSearch()}>搜索</button>
      </div>

      {/* Tab */}
      <div className="search-tabs">
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`search-tab ${activeTab === t.key ? 'active' : ''}`}
            onClick={() => setActiveTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* 结果 */}
      <div className="search-results">
        {loading && <><LoadingSkeleton /><LoadingSkeleton /></>}

        {!loading && results && !hasAnyResult && (
          <EmptyState icon="🔍" message="未找到相关内容" />
        )}

        {!loading && results && (
          <>
            {/* 用户 */}
            {showTab('users') && results.users.records.map((u: any) => (
              <div key={u.id} className="search-item"
                onClick={() => navigate(`/messages/chat?userId=${u.id}&name=${encodeURIComponent(u.nickname || '')}&avatar=${encodeURIComponent(u.avatar || '')}`)}>
                <div className="sr-avatar">
                  {u.avatar ? <img src={u.avatar} alt="" /> : <span>👤</span>}
                </div>
                <div className="sr-info">
                  <div className="sr-name">{u.nickname}</div>
                  <div className="sr-sub">{u.school || u.signature || ''}</div>
                </div>
                <span className="sr-tag">用户</span>
              </div>
            ))}

            {/* 动态 */}
            {showTab('posts') && results.posts.records.map((p: any) => (
              <div key={p.id} className="search-item"
                onClick={() => navigate(`/square/post/${p.id}`)}>
                <div className="sr-avatar">
                  {p.userAvatar ? <img src={p.userAvatar} alt="" /> : <span>📝</span>}
                </div>
                <div className="sr-info">
                  <div className="sr-name">{p.userNickname}</div>
                  <div className="sr-sub">{p.content}</div>
                </div>
                <span className="sr-tag">动态</span>
              </div>
            ))}

            {/* 搭子 */}
            {showTab('mates') && results.mates.records.map((m: any) => (
              <div key={m.id} className="search-item"
                onClick={() => navigate(`/mate/${m.id}`)}>
                <div className="sr-avatar"><span>🎯</span></div>
                <div className="sr-info">
                  <div className="sr-name">{m.title}</div>
                  <div className="sr-sub">{m.location || ''} · {m.activityTime || ''}</div>
                </div>
                <span className="sr-tag">搭子</span>
              </div>
            ))}
          </>
        )}

        {!loading && !results && (
          <div className="search-empty-hint">输入关键词搜索</div>
        )}
      </div>
    </div>
  );
}
