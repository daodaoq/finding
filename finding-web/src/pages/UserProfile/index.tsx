import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { userApi } from '../../api/user';
import { chatApi } from '../../api/chat';
import { useAuthStore } from '../../store/authStore';
import './index.css';

export default function UserProfilePage() {
  const { id } = useParams<{ id: string }>();
  const userId = Number(id);
  const [profile, setProfile] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const myId = useAuthStore((s) => s.user?.id);

  useEffect(() => {
    userApi.getProfile(userId).then((res) => {
      setProfile(res.data.data);
    }).catch(() => {}).finally(() => setLoading(false));
  }, [userId]);

  if (loading) return <div className="up-page"><div className="up-loading">加载中...</div></div>;
  if (!profile) return <div className="up-page"><div className="up-loading">用户不存在</div></div>;

  const handleChat = () => {
    if (!myId) return;
    const name = encodeURIComponent(profile.nickname || '');
    const avatar = encodeURIComponent(profile.avatar || '');
    navigate(`/messages/chat?userId=${userId}&name=${name}&avatar=${avatar}`);
  };

  return (
    <div className="up-page">
      <div className="up-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <span>用户信息</span>
      </div>

      <div className="up-card">
        <div className="up-avatar">
          {profile.avatar ? <img src={profile.avatar} alt="" /> : <span>👤</span>}
        </div>
        <div className="up-name">{profile.nickname}</div>
        {profile.signature && <div className="up-bio">{profile.signature}</div>}

        <div className="up-meta">
          {profile.school && (
            <div className="up-meta-item"><span>🏫</span> {profile.school}</div>
          )}
          {profile.city && (
            <div className="up-meta-item"><span>📍</span> {profile.city}</div>
          )}
          <div className="up-meta-item">
            <span>{profile.gender === 1 ? '♂️' : profile.gender === 2 ? '♀️' : '❓'}</span>
            {profile.gender === 1 ? '男' : profile.gender === 2 ? '女' : '未设置'}
          </div>
        </div>
      </div>

      {myId && userId !== myId && (
        <div className="up-actions">
          <button className="up-chat-btn" onClick={handleChat}>发消息</button>
        </div>
      )}
    </div>
  );
}
