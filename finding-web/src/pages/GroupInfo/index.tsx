import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { groupChatApi } from '../../api/groupChat';
import { useAuthStore } from '../../store/authStore';
import { showToast } from '../../components/Toast';
import ReportDialog from '../../components/ReportDialog';
import type { GroupChat, GroupMember, InvitableUser } from '../../types/groupChat';
import './index.css';

export default function GroupInfoPage() {
  const { id } = useParams<{ id: string }>();
  const groupId = Number(id);
  const [group, setGroup] = useState<GroupChat | null>(null);
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [showReport, setShowReport] = useState(false);
  const [editingAnnouncement, setEditingAnnouncement] = useState(false);
  const [announcementDraft, setAnnouncementDraft] = useState('');
  const [showInvite, setShowInvite] = useState(false);
  const [invitableUsers, setInvitableUsers] = useState<InvitableUser[]>([]);
  const [selectedInvite, setSelectedInvite] = useState<number[]>([]);
  const navigate = useNavigate();
  const myId = useAuthStore((s) => s.user?.id) || 0;
  const isOwner = group?.ownerId === myId;

  useEffect(() => {
    loadDetail();
  }, [groupId]);

  const loadDetail = async () => {
    try {
      const res = await groupChatApi.getGroupDetail(groupId);
      const data = res.data.data;
      setGroup(data);
      setMembers(data.members || []);
    } catch { showToast('加载群信息失败'); }
    finally { setLoading(false); }
  };

  const handleRemoveMember = async (userId: number) => {
    try {
      await groupChatApi.removeMember(groupId, userId);
      showToast('已移除');
      loadDetail();
    } catch (e: any) {
      showToast(e?.message || '操作失败');
    }
  };

  const handleSaveAnnouncement = async () => {
    try {
      await groupChatApi.updateAnnouncement(groupId, announcementDraft.trim());
      showToast('公告已更新');
      setEditingAnnouncement(false);
      loadDetail();
    } catch (e: any) { showToast(e?.message || '保存失败'); }
  };

  const openInvite = async () => {
    setShowInvite(true);
    setSelectedInvite([]);
    try {
      const res = await groupChatApi.getInvitableUsers(groupId);
      setInvitableUsers(res.data.data || []);
    } catch { setInvitableUsers([]); }
  };

  const handleInvite = async () => {
    try {
      await groupChatApi.addMembers(groupId, selectedInvite);
      showToast('已邀请加入');
      setShowInvite(false);
      loadDetail();
    } catch (e: any) { showToast(e?.message || '邀请失败'); }
  };

  const handleLeave = async () => {
    if (!window.confirm(isOwner ? '确定解散群聊？所有成员将被移除。' : '确定退出群聊？')) return;
    try {
      await groupChatApi.leaveOrDisband(groupId);
      showToast(isOwner ? '群聊已解散' : '已退出群聊');
      navigate('/messages', { replace: true });
    } catch (e: any) {
      showToast(e?.message || '操作失败');
    }
  };

  if (loading) return <div className="gi-page"><div className="gi-loading">加载中...</div></div>;
  if (!group) return <div className="gi-page"><div className="gi-loading">群聊不存在</div></div>;

  return (
    <div className="gi-page">
      <div className="gi-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <span>群聊信息</span>
        <button className="gi-report-btn" onClick={() => setShowReport(true)}>举报</button>
      </div>

      {/* 群基本信息 */}
      <div className="gi-basic">
        <div className="gi-avatar">
          {group.avatar ? <img src={group.avatar} alt="" /> : <span>👥</span>}
        </div>
        <div className="gi-name">{group.name}</div>
        <div className="gi-count">{members.length} 名成员</div>
      </div>

      {/* 群公告 */}
      <div className="gi-section-title">群公告</div>
      <div className="gi-announcement" style={{ padding: '0 16px 8px' }}>
        {editingAnnouncement ? (
          <div>
            <textarea
              value={announcementDraft}
              onChange={(e) => setAnnouncementDraft(e.target.value)}
              rows={3}
              placeholder="填写群公告..."
              style={{ width: '100%', border: '1px solid #eee', borderRadius: 8, padding: 10, fontSize: 14, boxSizing: 'border-box' }}
            />
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 8 }}>
              <button onClick={() => setEditingAnnouncement(false)}
                style={{ border: 'none', background: '#f0f0f0', color: '#666', padding: '6px 14px', borderRadius: 14, fontSize: 13 }}>
                取消
              </button>
              <button onClick={handleSaveAnnouncement}
                style={{ border: 'none', background: '#ff6b81', color: '#fff', padding: '6px 14px', borderRadius: 14, fontSize: 13 }}>
                保存
              </button>
            </div>
          </div>
        ) : (
          <div style={{ background: '#fff7f8', borderRadius: 8, padding: 12, fontSize: 14, color: '#555', position: 'relative' }}>
            {group.announcement || '暂无公告'}
            {isOwner && (
              <button
                onClick={() => { setAnnouncementDraft(group.announcement || ''); setEditingAnnouncement(true); }}
                style={{ position: 'absolute', top: 8, right: 10, border: 'none', background: 'none', color: '#ff6b81', fontSize: 13 }}
              >编辑</button>
            )}
          </div>
        )}
      </div>

      {/* 成员列表 */}
      <div className="gi-section-title">群成员（{members.length}）</div>
      <div className="gi-member-list">
        {members.map((m) => {
          const roleLabel = m.role === 2 ? '群主' : m.role === 1 ? '管理员' : '';
          return (
            <div key={m.userId} className="gi-member-item">
              <div className="gi-member-avatar" onClick={() => navigate(`/user/${m.userId}`)}>
                {m.avatar ? <img src={m.avatar} alt="" /> : <span>👤</span>}
              </div>
              <div className="gi-member-info">
                <span className="gi-member-name">
                  {m.nickname}
                  {m.userId === myId ? '（我）' : ''}
                </span>
                {roleLabel && <span className="gi-role-tag">{roleLabel}</span>}
              </div>
              {isOwner && m.userId !== myId && (
                <button className="gi-remove-btn" onClick={() => handleRemoveMember(m.userId)}>移除</button>
              )}
            </div>
          );
        })}
      </div>

      {/* 操作按钮 */}
      <div className="gi-actions">
        <button className="gi-invite-btn" onClick={openInvite}>邀请成员</button>
        <button className="gi-leave-btn" onClick={handleLeave}>
          {isOwner ? '解散群聊' : '退出群聊'}
        </button>
      </div>

      {/* 邀请成员弹层 */}
      {showInvite && (
        <div
          className="gi-modal-mask"
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', zIndex: 1000, display: 'flex', alignItems: 'flex-end', justifyContent: 'center' }}
          onClick={() => setShowInvite(false)}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{ width: '100%', maxWidth: 480, background: '#fff', borderRadius: '16px 16px 0 0', padding: 16, maxHeight: '70vh', overflowY: 'auto' }}
          >
            <h4 style={{ margin: '0 0 12px' }}>邀请成员</h4>
            {invitableUsers.length === 0 ? (
              <p style={{ color: '#999', textAlign: 'center', padding: 24 }}>
                暂无可邀请的人（可先去关注对方或发起私聊）
              </p>
            ) : invitableUsers.map((u) => (
              <label key={u.userId} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 0', borderBottom: '1px solid #f5f5f5' }}>
                <input
                  type="checkbox"
                  checked={selectedInvite.includes(u.userId)}
                  onChange={(e) => {
                    setSelectedInvite((prev) => e.target.checked
                      ? [...prev, u.userId]
                      : prev.filter((x) => x !== u.userId));
                  }}
                />
                <span style={{ fontSize: 15 }}>{u.nickname}</span>
              </label>
            ))}
            <button
              onClick={handleInvite}
              disabled={selectedInvite.length === 0}
              style={{
                width: '100%', marginTop: 16, padding: 12, border: 'none', borderRadius: 22,
                background: selectedInvite.length ? '#ff6b81' : '#eee', color: selectedInvite.length ? '#fff' : '#999', fontSize: 15,
              }}
            >
              邀请加入
            </button>
          </div>
        </div>
      )}

      {showReport && (
        <ReportDialog
          targetType="group"
          targetId={groupId}
          title="该群聊"
          onClose={() => setShowReport(false)}
        />
      )}
    </div>
  );
}
