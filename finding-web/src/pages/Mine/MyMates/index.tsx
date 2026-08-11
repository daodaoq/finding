import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { userApi } from '../../../api/user';
import { useAuthStore } from '../../../store/authStore';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import AppIcon from '../../../components/AppIcon';
import { showToast } from '../../../components/Toast';
import type { User } from '../../../types/user';
import '../subpage.css';

type TabKey = 'following' | 'mutual' | 'followers';

const TABS: { key: TabKey; label: string }[] = [
  { key: 'following', label: '关注' },
  { key: 'mutual', label: '互相关注' },
  { key: 'followers', label: '粉丝' },
];

export default function MyMatesPage() {
  const [searchParams] = useSearchParams();
  const initialTab = (searchParams.get('tab') as TabKey) || 'following';
  const [activeTab, setActiveTab] = useState<TabKey>(initialTab);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const currentUser = useAuthStore(s => s.user);

  useEffect(() => { loadUsers(); }, [activeTab]);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const api = activeTab === 'following' ? userApi.getFollowing
        : activeTab === 'mutual' ? userApi.getMutualFollows
        : userApi.getFollowers;
      const res = await api(currentUser!.id);
      setUsers(res.data.data.records);
    } catch { showToast('加载失败'); }
    finally { setLoading(false); }
  };

  const handleFollow = async (user: User) => {
    try {
      // 关注列表/互关列表里的都是已关注的 → 取关;粉丝列表按 isFollowed 判断
      const isFollowingNow = activeTab === 'following' || activeTab === 'mutual' ? true : user.isFollowed;
      if (isFollowingNow) {
        await userApi.unfollow(user.id);
      } else {
        await userApi.follow(user.id);
      }
      if (activeTab === 'following' || activeTab === 'mutual') {
        setUsers(prev => prev.filter(u => u.id !== user.id));
      } else {
        setUsers(prev => prev.map(u =>
          u.id === user.id ? { ...u, isFollowed: !u.isFollowed } : u
        ));
      }
    } catch { showToast('操作失败'); }
  };

  const getFollowLabel = (u: User) => {
    if (u.isFollowed) return '互相关注';
    if (activeTab === 'following') return '已关注';
    return '+ 关注';
  };

  const getFollowStyle = (u: User) => {
    if (u.isFollowed) return { color: '#29241f', borderColor: '#29241f' };
    if (activeTab === 'following') return {};
    return { background: '#29241f', color: '#fff', borderColor: '#29241f' };
  };

  const emptyMessage = () => {
    if (activeTab === 'following') return '还没有关注任何人';
    if (activeTab === 'mutual') return '还没有互相关注的人';
    return '还没有粉丝';
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>我的搭子</h2>
      </div>
      <div className="subpage-tabs">
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`tab ${activeTab === t.key ? 'active' : ''}`}
            onClick={() => setActiveTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}
        {!loading && users.map(u => (
          <div key={u.id} className="user-row">
            <div className="user-row-avatar">
              {u.avatar ? <img src={u.avatar} alt="" /> : <AppIcon name="user" size={20} />}
            </div>
            <div className="user-row-info">
              <span className="user-row-name">{u.nickname}</span>
              <span className="user-row-school">{u.school || ''}</span>
            </div>
            <button
              className="follow-btn-sm"
              style={getFollowStyle(u)}
              onClick={() => handleFollow(u)}
            >
              {getFollowLabel(u)}
            </button>
          </div>
        ))}
        {!loading && users.length === 0 && (
          <EmptyState message={emptyMessage()} />
        )}
      </div>
    </div>
  );
}
