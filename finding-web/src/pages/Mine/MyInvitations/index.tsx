import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mateApi } from '../../../api/mate';
import MateCard from '../../../components/MateCard';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import { showToast } from '../../../components/Toast';
import type { Mate } from '../../../types/mate';
import '../subpage.css';

export default function MyInvitationsPage() {
  const [mates, setMates] = useState<Mate[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => { loadMyInvitations(); }, []);

  const loadMyInvitations = async () => {
    try {
      const res = await mateApi.myInvitations(1, 50);
      setMates(res.data.data.records);
    } catch { showToast('加载失败'); }
    finally { setLoading(false); }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>我发布的邀约</h2>
      </div>
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}
        {!loading && mates.map(m => (
          <div key={m.id}>
            {(m.reviewStatus === 1 || m.reviewStatus === 2) && (
              <div style={{
                background: m.reviewStatus === 2 ? '#fff1f0' : '#fff7e6',
                color: m.reviewStatus === 2 ? '#f5222d' : '#d46b08',
                fontSize: 12, padding: '6px 16px',
              }}>
                {m.reviewStatus === 1
                  ? '审核中，暂不对他人可见'
                  : `审核未通过：${m.reviewReason || '未通过'}`}
              </div>
            )}
            <MateCard mate={m} onJoin={() => {}} onClick={id => navigate(`/mate/${id}`)} />
          </div>
        ))}
        {!loading && mates.length === 0 && <EmptyState message="还没有发布过邀约" />}
      </div>
    </div>
  );
}
