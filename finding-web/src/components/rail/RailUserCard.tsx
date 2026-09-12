import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { userApi } from '../../api/user';
import type { ProfileCompleteness } from '../../api/user';
import RailCard from './RailCard';
import './rail.css';

/** 登录:我的资料卡(头像/昵称/完整度进度);未登录:登录引导 + 发帖入口。 */
export default function RailUserCard() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const [completeness, setCompleteness] = useState<ProfileCompleteness | null>(null);

  useEffect(() => {
    if (!isLoggedIn) return;
    let alive = true;
    userApi.getCompleteness()
      .then((res) => { if (alive) setCompleteness(res.data.data); })
      .catch(() => {});
    return () => { alive = false; };
  }, [isLoggedIn]);

  if (!isLoggedIn) {
    return (
      <RailCard title="欢迎来到 Finding">
        <p className="rail-empty" style={{ lineHeight: 1.7 }}>登录后即可发布动态、寻找搭子、结识同校朋友。</p>
        <div className="rail-user-foot">
          <span className="rail-cta" onClick={() => navigate('/login')}>登录</span>
          <span className="rail-cta is-ghost" onClick={() => navigate('/create-post')}>发个帖</span>
        </div>
      </RailCard>
    );
  }

  const initial = (user?.nickname || '我').slice(0, 1);
  const score = completeness ? completeness.score : 0;
  const total = completeness ? completeness.total : 10;
  const missing = completeness?.missing || [];
  const pct = total > 0 ? Math.min(100, Math.round((score / total) * 100)) : 0;

  return (
    <RailCard title="我的">
      <button className="rail-user-top" onClick={() => navigate('/mine/profile')}>
        <span className="rail-avatar">{user?.avatar ? <img src={user.avatar} alt="" /> : initial}</span>
        <span className="rail-user-copy">
          <b>{user?.nickname || '未设置昵称'}</b>
          <span>{user?.school || ''}{user?.realNameVerified === 2 ? <em className="rail-verified">已认证</em> : null}</span>
        </span>
      </button>
      <div className="rail-progress"><i style={{ width: `${pct}%` }} /></div>
      <div className="rail-user-foot">
        <span>资料完整度 {score}/{total}</span>
        <span className="rail-cta" onClick={() => navigate('/mine/profile')}>{score >= total ? '查看主页' : '完善资料'}</span>
      </div>
      {missing.length > 0 && <p className="rail-empty" style={{ marginTop: 8 }}>还差:{missing.slice(0, 2).join('、')}</p>}
    </RailCard>
  );
}
