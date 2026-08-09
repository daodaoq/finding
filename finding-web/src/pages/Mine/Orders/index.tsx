import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mateApi } from '../../../api/mate';
import MateCard from '../../../components/MateCard';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import { showToast } from '../../../components/Toast';
import type { Mate } from '../../../types/mate';
import '../subpage.css';

const TABS = [
  { key: 'all', label: '全部' },
  { key: 'active', label: '进行中' },
  { key: 'ended', label: '已结束' },
] as const;

export default function OrdersPage() {
  const [mates, setMates] = useState<Mate[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('all');
  const navigate = useNavigate();

  useEffect(() => { loadOrders(); }, [activeTab]);

  const loadOrders = async () => {
    setLoading(true);
    try {
      const status = activeTab === 'active' ? 1 : activeTab === 'ended' ? 2 : undefined;
      const res = await mateApi.myJoined(1, 50, status);
      setMates(res.data.data.records);
    } catch { showToast('加载失败'); }
    finally { setLoading(false); }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>订单</h2>
      </div>
      <div className="subpage-tabs">
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`tab ${activeTab === t.key ? 'active' : ''}`}
            onClick={() => setActiveTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}
        {!loading && mates.map(m => (
          <MateCard key={m.id} mate={m} onJoin={() => {}} onClick={id => navigate(`/mate/${id}`)} />
        ))}
        {!loading && mates.length === 0 && (
          <EmptyState message="还没有预约成功的搭子" icon="🧾" />
        )}
      </div>
    </div>
  );
}
