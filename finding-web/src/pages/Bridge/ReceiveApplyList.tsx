import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bridgeApi } from '../../api/bridge';
import { useAuthStore } from '../../store/authStore';
import ConfirmDialog from '../../components/ConfirmDialog';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { showToast } from '../../components/Toast';
import AppIcon from '../../components/AppIcon';
import MatchOverlay from './components/MatchOverlay';
import type { ChatApply } from '../../types/bridge';
import './subpage.css';

const PAGE_SIZE = 20;

/** 全部 + 各状态(0待通过/1已通过/2已拒绝/3已撤回/4已过期) */
const STATUS_TABS = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待通过' },
  { key: '1', label: '已通过' },
  { key: '2', label: '已拒绝' },
  { key: '3', label: '已撤回' },
  { key: '4', label: '已过期' },
] as const;

export default function ReceiveApplyList() {
  const [applies, setApplies] = useState<ChatApply[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);
  const [filter, setFilter] = useState('all');
  const [rejectTarget, setRejectTarget] = useState<ChatApply | null>(null);
  // 匹配成功弹层:通过申请后展示
  const [matched, setMatched] = useState<ChatApply | null>(null);
  const navigate = useNavigate();
  const currentUser = useAuthStore((s) => s.user);

  const loadApplies = async (targetPage: number, statusKey: string, append = false) => {
    if (append) setLoadingMore(true);
    else setLoading(true);
    try {
      const status = statusKey === 'all' ? undefined : Number(statusKey);
      const res = await bridgeApi.receivedApplies(targetPage, PAGE_SIZE, status);
      const data = res.data.data;
      setApplies((prev) => (append ? [...prev, ...data.records] : data.records));
      setHasMore(data.hasMore);
      setPage(data.page);
    } catch (e) {
      showToast((e as Error)?.message || '加载申请列表失败');
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  useEffect(() => {
    loadApplies(1, filter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  const handleTabChange = (key: string) => {
    if (key !== filter) setFilter(key);
  };

  const handleApprove = async (apply: ChatApply) => {
    try {
      await bridgeApi.handleApply(apply.id, true);
      setApplies((prev) =>
        prev.map((a) =>
          a.id === apply.id ? { ...a, status: 1, statusDesc: '已通过' } : a
        )
      );
      // 展示「匹配成功」时刻
      setMatched(apply);
    } catch (e) {
      // 对方账号异常/已被处理等具体原因透出服务端文案,并刷新列表
      showToast((e as Error)?.message || '操作失败');
      loadApplies(1, filter);
    }
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
    } catch (e) {
      showToast((e as Error)?.message || '操作失败');
      setRejectTarget(null);
      loadApplies(1, filter);
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
        <h2>我收到的申请</h2>
      </div>

      {/* 状态筛选 Tab(服务端筛选) */}
      <div className="subpage-tabs">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.key}
            className={`subpage-tab ${filter === tab.key ? 'active' : ''}`}
            onClick={() => handleTabChange(tab.key)}
          >
            {tab.label}
          </button>
        ))}
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
                <AppIcon name="user" size={20} />
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
          <EmptyState icon="mail" message="还没有收到申请" />
        )}

        {/* 加载更多 */}
        {!loading && applies.length > 0 && (
          <div className="apply-loadmore">
            {hasMore ? (
              <button disabled={loadingMore} onClick={() => loadApplies(page + 1, filter, true)}>
                {loadingMore ? '加载中...' : '加载更多'}
              </button>
            ) : (
              <span className="apply-end">没有更多了</span>
            )}
          </div>
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

      {/* 匹配成功时刻 */}
      {matched && (
        <MatchOverlay
          apply={matched}
          myAvatar={currentUser?.avatar || ''}
          myNickname={currentUser?.nickname || ''}
          onGoChat={() => navigate(
            `/messages/chat?userId=${matched.fromUserId}` +
            `&name=${encodeURIComponent(matched.fromUserNickname || '')}` +
            `&avatar=${encodeURIComponent(matched.fromUserAvatar || '')}`
          )}
          onClose={() => setMatched(null)}
        />
      )}
    </div>
  );
}
