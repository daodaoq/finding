import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { historyApi, type HistoryRecord } from '../../../api/history';
import EmptyState from '../../../components/EmptyState';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import { formatSessionTime } from '../../../utils/format';
import '../subpage.css';
import './index.css';

export default function HistoryPage() {
  const [records, setRecords] = useState<HistoryRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    historyApi.list(1, 50)
      .then((res) => setRecords(res.data.data.records || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const go = (r: HistoryRecord) => {
    navigate(r.targetType === 'post' ? `/square/post/${r.targetId}` : `/user/${r.targetId}`);
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="subpage-back" onClick={() => navigate('/mine')}>←</button>
        <h2>浏览记录</h2>
      </div>
      {loading ? <LoadingSkeleton /> : records.length === 0 ? (
        <EmptyState icon="🕐" message="暂无浏览记录，去逛逛广场吧" />
      ) : (
        <div className="subpage-list">
          {records.map((r) => (
            <div key={`${r.targetType}-${r.targetId}`} className="history-row" onClick={() => go(r)}>
              <div className="history-avatar">
                {r.image ? <img src={r.image} alt="" /> : (r.targetType === 'post' ? '📄' : '👤')}
              </div>
              <div className="history-info">
                <span className="history-title">{r.title}</span>
                {r.subtitle && <span className="history-sub">{r.subtitle}</span>}
              </div>
              <span className="history-time">{formatSessionTime(r.createdAt)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
