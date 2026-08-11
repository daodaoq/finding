import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mateApi, type MateApplication } from '../../../api/mate';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import { showToast } from '../../../components/Toast';
import '../subpage.css';

const statusText: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已退出', 4: '候补中', 5: '活动已结束' };

export default function MyApplicationsPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<MateApplication[]>([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => { mateApi.myApplications(1, 50).then(r => setItems(r.data.data.records || [])).catch(e => showToast(e?.message || '加载失败')).finally(() => setLoading(false)); }, []);
  return <div className="subpage">
    <div className="subpage-header"><button className="back-btn" onClick={() => navigate('/mine')}>←</button><h2>我的搭子申请</h2></div>
    <div className="subpage-list">
      {loading && <LoadingSkeleton />}
      {!loading && items.map(item => <button key={`${item.invitationId}-${item.applyTime}`} className="subpage-list-item" onClick={() => navigate(`/mate/${item.invitationId}`)} style={{ textAlign: 'left', width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}><strong>{item.title}</strong><span>{statusText[item.applicationStatus] || '未知状态'}</span></div>
        <div style={{ fontSize: 13, color: '#766c62', marginTop: 6 }}>{item.activityTime ? new Date(item.activityTime).toLocaleString() : ''} · {item.location || '地点待定'}</div>
        {item.message && <div style={{ fontSize: 13, color: '#766c62', marginTop: 6 }}>留言：{item.message}</div>}
      </button>)}
      {!loading && items.length === 0 && <EmptyState message="还没有搭子申请记录" />}
    </div>
  </div>;
}
