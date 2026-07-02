import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bridgeApi } from '../../api/bridge';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import type { ChatApply } from '../../types/bridge';
import './subpage.css';

const STATUS_TABS = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待通过' },
  { key: '1', label: '已通过' },
  { key: '2', label: '已拒绝' },
] as const;

export default function SendApplyList() {
  const [applies, setApplies] = useState<ChatApply[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');
  const navigate = useNavigate();

  useEffect(() => {
    loadApplies();
  }, []);

  const loadApplies = async () => {
    setLoading(true);
    try {
      const res = await bridgeApi.sentApplies(1, 50);
      setApplies(res.data.data.records);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const filtered = filter === 'all'
    ? applies
    : applies.filter((a) => a.status === Number(filter));

  const handleRowClick = (apply: ChatApply) => {
    if (apply.status === 1 && apply.toUserId) {
      navigate(`/messages/chat?targetUserId=${apply.toUserId}&name=${encodeURIComponent(apply.toUserNickname || '')}&avatar=${encodeURIComponent(apply.toUserAvatar || '')}`);
    }
  };

  const formatTime = (dateStr: string): string => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - d.getTime();
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
    return d.toLocaleDateString('zh-CN');
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/bridge')}>←</button>
        <h2>我发出的申请</h2>
      </div>

      {/* 状态筛选 Tab */}
      <div className="subpage-tabs">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.key}
            className={`subpage-tab ${filter === tab.key ? 'active' : ''}`}
            onClick={() => setFilter(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 申请列表 */}
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}

        {!loading && filtered.map((apply) => (
          <div
            key={apply.id}
            className="apply-row"
            onClick={() => handleRowClick(apply)}
          >
            <div className="apply-avatar">
              {apply.toUserAvatar ? (
                <img src={apply.toUserAvatar} alt="" />
              ) : (
                <span>👤</span>
              )}
            </div>
            <div className="apply-info">
              <span className="apply-name">{apply.toUserNickname || '用户'}</span>
              <span className="apply-time">{formatTime(apply.applyTime)}</span>
            </div>
            <span className={`status-badge ${apply.status === 0 ? 'pending' : apply.status === 1 ? 'approved' : 'rejected'}`}>
              {apply.statusDesc}
            </span>
          </div>
        ))}

        {!loading && filtered.length === 0 && (
          <EmptyState icon="💌" message="还没有发出过申请" />
        )}
      </div>
    </div>
  );
}
