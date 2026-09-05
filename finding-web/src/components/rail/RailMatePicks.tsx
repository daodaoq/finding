import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mateApi } from '../../api/mate';
import type { Mate } from '../../types/mate';
import RailCard from './RailCard';
import './rail.css';

/** 搭子推荐:2 条横向迷你卡(封面图沿用 MateCard 的 picsum 种子约定)。 */
export default function RailMatePicks() {
  const navigate = useNavigate();
  const [mates, setMates] = useState<Mate[] | null>(null);

  useEffect(() => {
    let alive = true;
    mateApi.list({ page: 1, size: 2, sortBy: 'recommended' })
      .then((res) => { if (alive) setMates(res.data.data?.records ?? []); })
      .catch(() => { if (alive) setMates([]); });
    return () => { alive = false; };
  }, []);

  return (
    <RailCard title="搭子推荐" action={<span onClick={() => navigate('/mate')}>更多</span>}>
      {mates === null && <p className="rail-empty">加载中...</p>}
      {mates !== null && mates.length === 0 && <p className="rail-empty">暂无推荐</p>}
      {mates !== null && mates.map((m) => (
        <button key={m.id} className="rail-mate-item" onClick={() => navigate(`/mate/${m.id}`)}>
          <img src={`https://picsum.photos/seed/${encodeURIComponent(`${m.category}-${m.id}`)}/160/160`} alt="" loading="lazy" />
          <span className="rail-mate-copy">
            <b>{m.title}</b>
            <span>{m.categoryDesc || m.category} · {m.currentParticipants} 人想去</span>
          </span>
        </button>
      ))}
    </RailCard>
  );
}
