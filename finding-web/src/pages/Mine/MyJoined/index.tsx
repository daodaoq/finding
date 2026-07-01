import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mateApi } from '../../../api/mate';
import MateCard from '../../../components/MateCard';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import type { Mate } from '../../../types/mate';
import '../subpage.css';

export default function MyJoinedPage() {
  const [mates, setMates] = useState<Mate[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => { loadJoined(); }, []);

  const loadJoined = async () => {
    try {
      const res = await mateApi.myJoined(1, 50);
      setMates(res.data.data.records);
    } catch { /* */ }
    finally { setLoading(false); }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>我加入的搭子</h2>
      </div>
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}
        {!loading && mates.map(m => (
          <MateCard key={m.id} mate={m} onJoin={() => {}} onClick={id => navigate(`/mate/${id}`)} />
        ))}
        {!loading && mates.length === 0 && <EmptyState message="还没有加入任何搭子" />}
      </div>
    </div>
  );
}
