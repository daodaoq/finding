import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { reportApi, type MyReport } from '../../../api/report';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import { formatDateTime } from '../../../utils/format';
import '../subpage.css';
import './index.css';

const TYPE_LABEL: Record<string, string> = {
  message: '聊天消息',
  post: '动态',
  comment: '评论',
  mate: '搭子邀约',
  group: '群聊',
  user: '用户资料',
  resume: '情感简历/个人介绍',
};

const STATUS_MAP: Record<number, { label: string; cls: string }> = {
  0: { label: '待处理', cls: 'pending' },
  1: { label: '已处理', cls: 'handled' },
  2: { label: '已驳回', cls: 'rejected' },
};

export default function MyReports() {
  const [reports, setReports] = useState<MyReport[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    reportApi.myReports()
      .then((res) => setReports(res.data.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>我的投诉</h2>
      </div>

      {loading ? <LoadingSkeleton /> : reports.length === 0 ? (
        <EmptyState icon="flag" message="还没有提交过投诉" />
      ) : (
        <div className="subpage-list">
          {reports.map((r) => {
            const st = STATUS_MAP[r.status] || STATUS_MAP[0];
            return (
              <div key={r.id} className="myreport-item">
                <div className="myreport-top">
                  <span className="myreport-type">{TYPE_LABEL[r.targetType] || r.targetType}</span>
                  <span className={`myreport-status ${st.cls}`}>{st.label}</span>
                </div>
                <div className="myreport-reason">{r.reason}</div>
                {r.contentSnapshot && (
                  <div className="myreport-snapshot">{r.contentSnapshot}</div>
                )}
                {r.handleNote && (
                  <div className="myreport-handle">处理意见：{r.handleNote}</div>
                )}
                <div className="myreport-time">
                  提交于 {formatDateTime(r.createdAt)}
                  {r.handleTime ? ` · 处理于 ${formatDateTime(r.handleTime)}` : ''}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
