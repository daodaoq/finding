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
            {m.status === 1 && !m.isExpired && <div style={{ display: 'flex', gap: 8, padding: '0 16px 12px' }}>
              <button className="subpage-action-btn" onClick={() => navigate(`/create-mate/${m.id}`)}>编辑</button>
              <button className="subpage-action-btn" onClick={async () => { if (!window.confirm('关闭后将停止新的报名，确认继续吗？')) return; try { await mateApi.close(m.id); showToast('已停止报名'); loadMyInvitations(); } catch (e: any) { showToast(e?.message || '操作失败'); } }}>停止报名</button>
              <button className="subpage-action-btn danger" onClick={async () => { if (!window.confirm('取消后活动将不再举行，确认继续吗？')) return; try { await mateApi.cancel(m.id); showToast('活动已取消'); loadMyInvitations(); } catch (e: any) { showToast(e?.message || '操作失败'); } }}>取消活动</button>
            </div>}
          </div>
        ))}
        {!loading && mates.length === 0 && <EmptyState message="还没有发布过邀约" />}
      </div>
    </div>
  );
}
