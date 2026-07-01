import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { userApi } from '../../../api/user';
import { useAuthStore } from '../../../store/authStore';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import type { User } from '../../../types/user';
import '../subpage.css';

export default function MyMatesPage() {
  const [searchParams] = useSearchParams();
  const initialTab = (searchParams.get('tab') as 'following' | 'followers') || 'following';
  const [activeTab, setActiveTab] = useState<'following' | 'followers'>(initialTab);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const currentUser = useAuthStore(s => s.user);

  useEffect(() => { loadUsers(); }, [activeTab]);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const api = activeTab === 'following' ? userApi.getFollowing : userApi.getFollowers;
      const res = await api(currentUser!.id);
      setUsers(res.data.data.records);
    } catch { /* */ }
    finally { setLoading(false); }
  };

  const handleFollow = async (user: User) => {
    try {
      await userApi.follow(user.id);
      if (activeTab === 'following') {
        // 关注/互关 → 取消关注 → 从列表移除
        setUsers(prev => prev.filter(u => u.id !== user.id));
      } else {
        // 粉丝列表：点击 +关注/互相关注 → 切换
        setUsers(prev => prev.map(u =>
          u.id === user.id ? { ...u, isFollowed: !u.isFollowed } : u
        ));
      }
    } catch { /* */ }
  };

  const getFollowLabel = (u: User) => {
    if (u.isFollowed) return '互相关注';
    if (activeTab === 'following') return '已关注';
    return '+ 关注';
  };

  const getFollowStyle = (u: User) => {
    if (u.isFollowed) return { color: '#ff6b81', borderColor: '#ff6b81' };
    if (activeTab === 'following') return {};
    return { background: '#ff6b81', color: '#fff', borderColor: '#ff6b81' };
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>我的搭子</h2>
      </div>
      <div className="subpage-tabs">
        <button className={`tab ${activeTab === 'following' ? 'active' : ''}`}
          onClick={() => setActiveTab('following')}>关注</button>
        <button className={`tab ${activeTab === 'followers' ? 'active' : ''}`}
          onClick={() => setActiveTab('followers')}>粉丝</button>
      </div>
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}
        {!loading && users.map(u => (
          <div key={u.id} className="user-row">
            <div className="user-row-avatar">
              {u.avatar ? <img src={u.avatar} alt="" /> : <span>👤</span>}
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
          <EmptyState message={activeTab === 'following' ? '还没有关注任何人' : '还没有粉丝'} />
        )}
      </div>
    </div>
  );
}
