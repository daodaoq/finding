import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../../../api/user';
import { useAuthStore } from '../../../store/authStore';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import type { User } from '../../../types/user';
import '../subpage.css';

export default function MyMatesPage() {
  const [activeTab, setActiveTab] = useState<'following' | 'followers'>('following');
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
        // 关注列表：点击"已关注"→取消关注→从列表移除
        setUsers(prev => prev.filter(u => u.id !== user.id));
      } else {
        // 粉丝列表：点击切换互关/关注状态
        setUsers(prev => prev.map(u =>
          u.id === user.id ? { ...u, isFollowed: !u.isFollowed } : u
        ));
      }
    } catch { /* */ }
  };

  const getFollowLabel = (u: User) => {
    if (activeTab === 'following') return '已关注';
    return u.isFollowed ? '互相关注' : '+ 关注';
  };

  const getFollowStyle = (u: User) => {
    if (activeTab === 'following' || u.isFollowed) return {};
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
