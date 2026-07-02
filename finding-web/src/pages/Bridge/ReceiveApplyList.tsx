import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bridgeApi } from '../../api/bridge';
import ConfirmDialog from '../../components/ConfirmDialog';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { showToast } from '../../components/Toast';
import type { ChatApply } from '../../types/bridge';
import './subpage.css';

export default function ReceiveApplyList() {
  const [applies, setApplies] = useState<ChatApply[]>([]);
  const [loading, setLoading] = useState(true);
  const [rejectTarget, setRejectTarget] = useState<ChatApply | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    loadApplies();
  }, []);

  const loadApplies = async () => {
    setLoading(true);
    try {
      const res = await bridgeApi.receivedApplies(1, 50);
      setApplies(res.data.data.records);
    } catch { showToast('加载申请列表失败'); }
    finally { setLoading(false); }
  };

  const handleApprove = async (apply: ChatApply) => {
    try {
      await bridgeApi.handleApply(apply.id, true);
      setApplies((prev) =>
        prev.map((a) =>
          a.id === apply.id ? { ...a, status: 1, statusDesc: '已通过' } : a
        )
      );
      showToast('已通过申请，可以开始聊天了');
      // 立即跳转到私聊页面
      navigate(`/messages/chat?userId=${apply.fromUserId}&name=${encodeURIComponent(apply.fromUserNickname || '')}&avatar=${encodeURIComponent(apply.fromUserAvatar || '')}`);
    } catch { showToast('操作失败'); }
  };

  const handleReject = async () => {
    if (!rejectTarget) return;
    try {
      await bridgeApi.handleApply(rejectTarget.id, false);
      setApplies((prev) =>
        prev.map((a) =>
          a.id === rejectTarget.id ? { ...a, status: 2, statusDesc: '已拒绝' } : a
        )
      );
      setRejectTarget(null);
      showToast('已拒绝申请');
    } catch { showToast('操作失败'); }
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
        <h2>我收到的申请</h2>
      </div>

      {/* 申请列表 */}
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}

        {!loading && applies.map((apply) => (
          <div key={apply.id} className="apply-row">
            <div className="apply-avatar">
              {apply.fromUserAvatar ? (
                <img src={apply.fromUserAvatar} alt="" />
              ) : (
                <span>👤</span>
              )}
            </div>
            <div className="apply-info">
              <span className="apply-name">{apply.fromUserNickname || '用户'}</span>
              <span className="apply-time">{formatTime(apply.applyTime)}</span>
              {apply.remark && <span className="apply-remark">{apply.remark}</span>}
            </div>

            {apply.status === 0 ? (
              <div className="apply-actions">
                <button className="btn-approve" onClick={() => handleApprove(apply)}>
                  通过
                </button>
                <button className="btn-reject" onClick={() => setRejectTarget(apply)}>
                  拒绝
                </button>
              </div>
            ) : (
              <span className={`status-badge ${apply.status === 1 ? 'approved' : 'rejected'}`}>
                {apply.statusDesc}
              </span>
            )}
          </div>
        ))}

        {!loading && applies.length === 0 && (
          <EmptyState icon="📬" message="还没有收到申请" />
        )}
      </div>

      {/* 拒绝确认弹窗 */}
      <ConfirmDialog
        visible={rejectTarget !== null}
        title="拒绝申请"
        message={`确定拒绝 ${rejectTarget?.fromUserNickname || '该用户'} 的聊天申请吗？`}
        confirmText="拒绝"
        cancelText="取消"
        onConfirm={handleReject}
        onCancel={() => setRejectTarget(null)}
      />
    </div>
  );
}
