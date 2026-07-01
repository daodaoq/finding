import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { mateApi } from '../../api/mate';
import { useAuthStore } from '../../store/authStore';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import LoginModal from '../../components/LoginModal';
import ConfirmDialog from '../../components/ConfirmDialog';
import LoadingSkeleton from '../../components/LoadingSkeleton';
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
      try {
        await mateApi.join(mateId);
        setMate(prev => prev ? {
          ...prev,
          hasJoined: true,
          currentParticipants: prev.currentParticipants + 1
        } : null);
        showToast('申请已发送');
      } catch { showToast('加入失败'); }
    });
  };

  const handleLeave = () => setShowLeaveConfirm(true);

  const confirmLeave = async () => {
    setShowLeaveConfirm(false);
    setLeaving(true);
    try {
      await mateApi.leave(mateId);
      setMate(prev => prev ? {
        ...prev,
        hasJoined: false,
        currentParticipants: Math.max(0, prev.currentParticipants - 1)
      } : null);
      showToast('已退出搭子活动');
    } catch { showToast('退出失败'); }
    finally { setLeaving(false); }
  };

  const isOwner = user?.id != null && mate?.userId === user.id;

  if (loading) return <div className="md-page"><LoadingSkeleton /></div>;
  if (!mate) return null;

  return (
    <div className="md-page">
      {/* Header */}
      <div className="md-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h3>搭子详情</h3>
      </div>

      {/* 活动头部 */}
      <div className="md-hero">
        <span className="md-category-badge">{mate.categoryDesc || mate.category}</span>
        <h2 className="md-title">{mate.title}</h2>
        <span className={`md-status-tag status-${mate.status}`}>
          {mate.status === 2 ? '已关闭' : mate.isFull ? '已满员' : '招募中'}
        </span>
      </div>

      {/* 活动信息 */}
      <div className="md-info-card">
        <div className="md-info-row">
          <span className="md-info-icon">🕐</span>
          <div>
            <span className="md-info-label">活动时间</span>
            <span className="md-info-value">{formatFullDate(mate.activityTime)}</span>
          </div>
        </div>
        <div className="md-info-row">
          <span className="md-info-icon">📍</span>
          <div>
            <span className="md-info-label">活动地点</span>
            <span className="md-info-value">{mate.location || '未指定'}</span>
          </div>
        </div>
        <div className="md-info-row">
          <span className="md-info-icon">👥</span>
          <div>
            <span className="md-info-label">参与人数</span>
            <span className="md-info-value">
              {mate.currentParticipants}/{mate.maxParticipants} 人
              {mate.isFull && <span className="md-full-badge">已满</span>}
            </span>
          </div>
        </div>
        {mate.distanceKm != null && (
          <div className="md-info-row">
            <span className="md-info-icon">📏</span>
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
              {mate.author.avatar ? <img src={mate.author.avatar} alt="" /> : <span>👤</span>}
            </div>
            <div>
              <span className="md-author-name">{mate.author.nickname}</span>
              <span className="md-author-school">{mate.author.school || ''}</span>
            </div>
            {isOwner && <span className="md-owner-badge">我发布的</span>}
          </div>
        </div>
      )}

      {/* 底部操作栏 */}
      <div className="md-bottom-bar">
        {isOwner ? (
          <button className="md-btn md-btn-disabled" disabled>这是我发布的搭子</button>
        ) : mate.hasJoined ? (
          <button className="md-btn md-btn-leave" onClick={handleLeave} disabled={leaving}>
            {leaving ? '退出中...' : '退出搭子活动'}
          </button>
        ) : mate.status === 1 && !mate.isFull ? (
          <button className="md-btn md-btn-join" onClick={handleJoin}>
            申请加入
          </button>
        ) : (
          <button className="md-btn md-btn-disabled" disabled>
            {mate.status === 2 ? '活动已关闭' : '人数已满'}
          </button>
        )}
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
    </div>
  );
}

function formatFullDate(dateStr: string): string {
  if (!dateStr) return '未指定';
  const d = new Date(dateStr);
  const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 周${weekDays[d.getDay()]} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}
