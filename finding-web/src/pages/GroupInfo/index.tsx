import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { groupChatApi } from '../../api/groupChat';
import { useAuthStore } from '../../store/authStore';
import { showToast } from '../../components/Toast';
import type { GroupChat, GroupMember } from '../../types/groupChat';
import './index.css';

export default function GroupInfoPage() {
  const { id } = useParams<{ id: string }>();
  const groupId = Number(id);
  const [group, setGroup] = useState<GroupChat | null>(null);
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [loading, setLoading] = useState(true);
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
      </div>

      {/* 群基本信息 */}
      <div className="gi-basic">
        <div className="gi-avatar">
          {group.avatar ? <img src={group.avatar} alt="" /> : <span>👥</span>}
        </div>
        <div className="gi-name">{group.name}</div>
        <div className="gi-count">{members.length} 名成员</div>
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
        <button className="gi-leave-btn" onClick={handleLeave}>
          {isOwner ? '解散群聊' : '退出群聊'}
        </button>
      </div>
    </div>
  );
}
