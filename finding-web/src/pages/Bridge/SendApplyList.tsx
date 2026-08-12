import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bridgeApi } from '../../api/bridge';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { showToast } from '../../components/Toast';
import AppIcon from '../../components/AppIcon';
import { useApplyList, APPLY_STATUS_TABS } from '../../hooks/useApplyList';
import { formatRelativeTime } from '../../utils/format';
import type { ChatApply } from '../../types/bridge';
import './subpage.css';

export default function SendApplyList() {
  const navigate = useNavigate();
  const {
    applies, setApplies, loading, loadingMore, page, hasMore, filter,
    handleTabChange, loadPage,
  } = useApplyList(bridgeApi.sentApplies);

  const handleRowClick = (apply: ChatApply) => {
    if (apply.status === 1 && apply.toUserId) {
      navigate(`/messages/chat?userId=${apply.toUserId}&name=${encodeURIComponent(apply.toUserNickname || '')}&avatar=${encodeURIComponent(apply.toUserAvatar || '')}`);
    }
  };

  /** 撤回待处理申请 */
  const handleWithdraw = async (apply: ChatApply) => {
    try {
      await bridgeApi.withdrawApply(apply.id);
      setApplies((prev) => prev.map((a) =>
        a.id === apply.id ? { ...a, status: 3, statusDesc: '已撤回' } : a));
      showToast('已撤回申请');
    } catch (e) {
      // 已被处理等具体原因透出服务端文案
      showToast((e as Error)?.message || '撤回失败');
    }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/bridge')}>←</button>
        <h2>我发出的申请</h2>
      </div>

      {/* 状态筛选 Tab(服务端筛选) */}
      <div className="subpage-tabs">
        {APPLY_STATUS_TABS.map((tab) => (
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
          <div
            key={apply.id}
            className="apply-row"
            onClick={() => handleRowClick(apply)}
          >
            <div className="apply-avatar">
              {apply.toUserAvatar ? (
                <img src={apply.toUserAvatar} alt="" />
              ) : (
                <AppIcon name="user" size={20} />
              )}
            </div>
            <div className="apply-info">
              <span className="apply-name">{apply.toUserNickname || '用户'}</span>
              <span className="apply-time">{formatRelativeTime(apply.applyTime)}</span>
            </div>
            <div className="apply-right">
              {apply.status === 0 && (
                <button className="apply-withdraw" onClick={(e) => { e.stopPropagation(); handleWithdraw(apply); }}>
                  撤回
                </button>
              )}
              <span className={`status-badge ${apply.status === 0 ? 'pending' : apply.status === 1 ? 'approved' : 'rejected'}`}>
                {apply.statusDesc}
              </span>
            </div>
          </div>
        ))}

        {!loading && applies.length === 0 && (
          <EmptyState icon="send" message="还没有发出过申请" />
        )}

        {/* 加载更多 */}
        {!loading && applies.length > 0 && (
          <div className="apply-loadmore">
            {hasMore ? (
              <button disabled={loadingMore} onClick={() => loadPage(page + 1, true)}>
                {loadingMore ? '加载中...' : '加载更多'}
              </button>
            ) : (
              <span className="apply-end">没有更多了</span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
