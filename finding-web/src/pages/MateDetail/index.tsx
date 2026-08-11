import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { mateApi, type Participant } from '../../api/mate';
import { useAuthStore } from '../../store/authStore';
import ReportDialog from '../../components/ReportDialog';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import LoginModal from '../../components/LoginModal';
import ConfirmDialog from '../../components/ConfirmDialog';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import AppIcon from '../../components/AppIcon';
import { showToast } from '../../components/Toast';
import type { Mate } from '../../types/mate';
import './index.css';

export default function MateDetailPage() {
  const { id } = useParams<{ id: string }>();
  const mateId = Number(id);
  const navigate = useNavigate();
  const user = useAuthStore(s => s.user);
  const { showLogin, requireLogin, handleLoginSuccess, handleClose } = useRequireLogin();

  const [mate, setMate] = useState<Mate | null>(null);
  const [loading, setLoading] = useState(true);
  const [leaving, setLeaving] = useState(false);
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false);
  const [showReport, setShowReport] = useState(false);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [managing, setManaging] = useState(false);

  useEffect(() => { loadDetail(); }, [mateId]);

  const loadDetail = async () => {
    try {
      const res = await mateApi.detail(mateId);
      setMate(res.data.data);
    } catch { navigate(-1); }
    finally { setLoading(false); }
  };

  const handleJoin = () => {
    requireLogin(async () => {
      const message = window.prompt('可以给发起人留一句话（选填，最多500字）', '') ?? undefined;
      if (message !== undefined && message.length > 500) { showToast('留言不能超过500字'); return; }
      try {
        await mateApi.join(mateId, message);
        showToast('申请已发送');
        loadDetail(); // 刷新剩余名额/报名状态(满员时可能进入候补)
      } catch (e: any) { showToast(e?.message || '加入失败'); }
    });
  };

  const handleLeave = () => setShowLeaveConfirm(true);

  const confirmLeave = async () => {
    setShowLeaveConfirm(false);
    setLeaving(true);
    try {
      await mateApi.leave(mateId);
      await loadDetail();
      showToast('已退出搭子活动');
    } catch { showToast('退出失败'); }
    finally { setLeaving(false); }
  };

  const isOwner = user?.id != null && mate?.userId === user.id;

  // 发起人拉取申请人列表
  useEffect(() => {
    if (!isOwner && mate?.myApplicationStatus !== 1) return;
    mateApi.participants(mateId).then((res) => setParticipants(res.data.data || [])).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOwner, mate?.myApplicationStatus, mateId]);

  const handleJoinRequest = async (participantId: number, accept: boolean) => {
    setManaging(true);
    try {
      await mateApi.handleJoin(mateId, participantId, accept);
      showToast(accept ? '已通过申请' : '已拒绝申请');
      const res = await mateApi.participants(mateId);
      setParticipants(res.data.data || []);
      loadDetail(); // 刷新参与人数
    } catch (e: any) { showToast(e?.message || '操作失败，请重试'); }
    finally { setManaging(false); }
  };

  if (loading) return <div className="md-page"><LoadingSkeleton /></div>;
  if (!mate) return null;

  return (
    <div className="md-page">
      {/* Header */}
      <div className="md-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h3>搭子详情</h3>
        {!isOwner && (
          <button className="md-report-btn" onClick={() => setShowReport(true)}>举报</button>
        )}
      </div>

      {/* 活动头部 */}
      <div className="md-hero">
        <span className="md-category-badge">{mate.categoryDesc || mate.category}</span>
        <h2 className="md-title">{mate.title}</h2>
        <span className={`md-status-tag status-${mate.status}`}>
          {mate.status === 0 ? '已取消' : mate.status === 2 ? '已关闭' : mate.isExpired ? '已过期' : mate.isFull ? '已满员' : '招募中'}
        </span>
      </div>

      {/* 活动信息 */}
      <div className="md-info-card">
        <div className="md-info-row">
          <span className="md-info-icon"><AppIcon name="clock" size={18} /></span>
          <div>
            <span className="md-info-label">活动时间</span>
            <span className="md-info-value">{formatFullDate(mate.activityTime)}（截止报名）</span>
          </div>
        </div>
        <div className="md-info-row">
          <span className="md-info-icon"><AppIcon name="location" size={18} /></span>
          <div>
            <span className="md-info-label">活动地点</span>
            <span className="md-info-value">{mate.location || '未指定'}</span>
          </div>
        </div>
        <div className="md-info-row">
          <span className="md-info-icon"><AppIcon name="users" size={18} /></span>
          <div>
            <span className="md-info-label">参与人数</span>
            <span className="md-info-value">
              {mate.currentParticipants}/{mate.maxParticipants} 人 · 剩余 {mate.remainingSlots ?? 0} 名
              {mate.isFull && <span className="md-full-badge">已满·可候补</span>}
            </span>
          </div>
        </div>
        {mate.distanceKm != null && (
          <div className="md-info-row">
            <span className="md-info-icon"><AppIcon name="ruler" size={18} /></span>
            <div>
              <span className="md-info-label">距离</span>
              <span className="md-info-value">{mate.distanceKm.toFixed(1)} km</span>
            </div>
          </div>
        )}
      </div>

      {/* 活动描述 */}
      {mate.description && (
        <div className="md-section">
          <h4 className="md-section-title">活动描述</h4>
          <p className="md-desc">{mate.description}</p>
        </div>
      )}

      {/* 发起人信息 */}
      {mate.author && !mate.isAnonymous && (
        <div className="md-section">
          <h4 className="md-section-title">发起人</h4>
          <div className="md-author-row">
            <div className="md-author-avatar">
              {mate.author.avatar ? <img src={mate.author.avatar} alt="" /> : <AppIcon name="user" size={20} />}
            </div>
            <div>
              <span className="md-author-name">{mate.author.nickname}</span>
              <span className="md-author-school">{mate.author.school || ''}</span>
            </div>
            {isOwner && <span className="md-owner-badge">我发布的</span>}
          </div>
        </div>
      )}

      {/* 发起人:申请管理 */}
      {(isOwner || mate.myApplicationStatus === 1) && (
        <div className="md-section">
          <h4 className="md-section-title">
            {isOwner ? `申请管理（${participants.filter(p => p.status === 0 || p.status === 4).length} 待处理）` : '已通过成员'}
          </h4>
          {participants.length === 0 ? (
            <p className="md-desc" style={{ color: '#999' }}>暂无申请</p>
          ) : participants.map((p) => (
            <div key={p.participantId} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 0', borderBottom: '1px solid #f0f0f0' }}>
              <div style={{ width: 36, height: 36, borderRadius: '50%', overflow: 'hidden', flexShrink: 0, background: '#f0f0f0' }}>
                {p.avatar
                  ? <img src={p.avatar} alt="" style={{ width: 36, height: 36, objectFit: 'cover' }} />
                  : <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: 36, height: 36 }}><AppIcon name="user" size={20} /></span>}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontWeight: 600 }}>{p.nickname || `用户${p.userId}`}</div>
                {p.message && (
                  <div style={{ fontSize: 12, color: '#999', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {p.message}
                  </div>
                )}
              </div>
              {isOwner && (p.status === 0 || p.status === 4) ? (
                <div style={{ display: 'flex', gap: 6, flexShrink: 0, alignItems: 'center' }}>
                  {p.status === 4 && <span style={{ fontSize: 11, color: '#f59e0b', flexShrink: 0 }}>候补</span>}
                  <button
                    onClick={() => handleJoinRequest(p.participantId, true)} disabled={managing}
                    style={{ border: 'none', background: '#52c41a', color: '#fff', padding: '4px 12px', borderRadius: 14, fontSize: 13 }}
                  >通过</button>
                  <button
                    onClick={() => handleJoinRequest(p.participantId, false)} disabled={managing}
                    style={{ border: 'none', background: '#f5222d', color: '#fff', padding: '4px 12px', borderRadius: 14, fontSize: 13 }}
                  >拒绝</button>
                </div>
              ) : (
                <span style={{ fontSize: 12, flexShrink: 0, color: p.status === 1 ? '#52c41a' : '#999' }}>
                  {p.status === 1 ? '已通过' : p.status === 3 ? '已退出' : '已拒绝'}
                </span>
              )}
            </div>
          ))}
        </div>
      )}

      {/* 底部操作栏:按我的报名状态/活动状态渲染 */}
      <div className="md-bottom-bar">
        {isOwner ? (
          <button className="md-btn md-btn-disabled" disabled>这是我发布的搭子</button>
        ) : (() => {
          const my = mate.myApplicationStatus;
          if (mate.status !== 1 || mate.isExpired) {
            return (
              <button className="md-btn md-btn-disabled" disabled>
                {mate.status === 0 ? '活动已取消' : mate.status === 2 ? '活动已关闭' : mate.isExpired ? '活动已过期' : '不可加入'}
              </button>
            );
          }
          if (my === 0) return <button className="md-btn md-btn-disabled" disabled>待审核中</button>;
          if (my === 4) return <button className="md-btn md-btn-disabled" disabled>候补中</button>;
          if (my === 2 || my === 3) return <button className="md-btn md-btn-disabled" disabled>已申请过，无法重复加入</button>;
          if (my === 1) {
            return (
              <button className="md-btn md-btn-leave" onClick={handleLeave} disabled={leaving}>
                {leaving ? '退出中...' : '退出搭子活动'}
              </button>
            );
          }
          return (
            <button className="md-btn md-btn-join" onClick={handleJoin}>
              {mate.isFull ? '加入候补' : '申请加入'}
            </button>
          );
        })()}
      </div>

      <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />
      <ConfirmDialog
        visible={showLeaveConfirm}
        title="退出搭子活动"
        message="确定要退出这个搭子活动吗？退出后将不再参与本次活动。"
        confirmText="确定退出"
        danger
        onConfirm={confirmLeave}
        onCancel={() => setShowLeaveConfirm(false)}
      />

      {showReport && (
        <ReportDialog
          targetType="mate"
          targetId={mate.id}
          title="该搭子邀约"
          onClose={() => setShowReport(false)}
        />
      )}
    </div>
  );
}

function formatFullDate(dateStr: string): string {
  if (!dateStr) return '未指定';
  const d = new Date(dateStr);
  const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 周${weekDays[d.getDay()]} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}
