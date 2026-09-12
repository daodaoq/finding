import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../../api/user';
import { useAuthStore } from '../../store/authStore';
import { showToast } from '../Toast';
import RailCard from './RailCard';
import type { User } from '../../types/user';
import './rail.css';
import { previewHandler } from '../../utils/preview';

/** 帖子作者卡:资料入口 + 关注/私信。作者是自己时不显示操作按钮。 */
export default function RailAuthorCard({ author }: { author: User }) {
  const navigate = useNavigate();
  const me = useAuthStore((s) => s.user);
  const [followed, setFollowed] = useState(!!author.isFollowed);
  const [acting, setActing] = useState(false);
  const isSelf = me?.id === author.id;

  const toggleFollow = async () => {
    if (!me) { navigate('/login'); return; }
    if (acting) return;
    setActing(true);
    try {
      if (followed) { await userApi.unfollow(author.id); setFollowed(false); }
      else { await userApi.follow(author.id); setFollowed(true); }
    } catch { showToast('操作失败'); }
    finally { setActing(false); }
  };

  const chat = () => {
    if (!me) { navigate('/login'); return; }
    navigate(`/messages/chat?userId=${author.id}&name=${encodeURIComponent(author.nickname)}&avatar=${encodeURIComponent(author.avatar || '')}`);
  };

  return (
    <RailCard title="作者">
      <button className="rail-user-top" onClick={() => navigate(`/user/${author.id}`)}>
        <span className="rail-avatar" onClick={previewHandler(author.avatar)}>{author.avatar ? <img src={author.avatar} alt="" /> : author.nickname.slice(0, 1)}</span>
        <span className="rail-user-copy">
          <b>{author.nickname}</b>
          <span>{author.school || ''}{author.realNameVerified === 2 ? <em className="rail-verified">已认证</em> : null}</span>
        </span>
      </button>
      {!isSelf && (
        <div className="rail-author-actions">
          <button className={`rail-author-btn ${followed ? 'is-followed' : ''}`} onClick={toggleFollow} disabled={acting}>
            {followed ? '已关注' : '+ 关注'}
          </button>
          <button className="rail-author-btn is-ghost" onClick={chat}>私信</button>
        </div>
      )}
    </RailCard>
  );
}
