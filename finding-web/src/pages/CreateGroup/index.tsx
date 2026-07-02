import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { groupChatApi } from '../../api/groupChat';
import { userApi } from '../../api/user';
import { useAuthStore } from '../../store/authStore';
import { showToast } from '../../components/Toast';
import type { InvitableUser } from '../../types/groupChat';
import './index.css';

export default function CreateGroupPage() {
  const [users, setUsers] = useState<InvitableUser[]>([]);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [groupName, setGroupName] = useState('');
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const navigate = useNavigate();
  const myId = useAuthStore((s) => s.user?.id) || 0;

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    try {
      // 获取关注列表作为可选用户
      const res = await userApi.getFollowing(myId);
      const list = (res.data?.data?.records || []).map((u: any) => ({
        userId: u.id || u.userId,
        nickname: u.nickname,
        avatar: u.avatar,
      }));
      setUsers(list);
    } catch { /**/ }
    finally { setLoading(false); }
  };

  const toggleUser = (uid: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(uid)) next.delete(uid); else next.add(uid);
      return next;
    });
  };

  const handleCreate = async () => {
    if (!groupName.trim()) { showToast('请输入群名称'); return; }
    if (selected.size < 2) { showToast('请至少选择2个成员'); return; }
    setCreating(true);
    try {
      const res = await groupChatApi.createGroup(groupName.trim(), Array.from(selected));
      const group = res.data.data;
      showToast('群聊创建成功');
      navigate(`/messages/group-chat/${group.id}?name=${encodeURIComponent(group.name)}`, { replace: true });
    } catch (e: any) {
      showToast(e?.message || '创建失败');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="create-group-page">
      <div className="create-group-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <span>创建群聊</span>
        <button className="create-btn" onClick={handleCreate} disabled={creating}>
          {creating ? '创建中' : '创建'}
        </button>
      </div>

      <div className="group-name-input">
        <input
          placeholder="输入群名称"
          value={groupName}
          onChange={(e) => setGroupName(e.target.value)}
          maxLength={30}
        />
      </div>

      <div className="selected-count">
        已选 {selected.size} 人
      </div>

      <div className="user-list">
        {loading && <div className="loading-text">加载中...</div>}
        {users.map((u) => (
          <div
            key={u.userId}
            className={`user-item ${selected.has(u.userId) ? 'selected' : ''}`}
            onClick={() => toggleUser(u.userId)}
          >
            <div className="user-avatar">
              {u.avatar ? <img src={u.avatar} alt="" /> : <span>👤</span>}
            </div>
            <span className="user-name">{u.nickname}</span>
            <div className={`checkbox ${selected.has(u.userId) ? 'checked' : ''}`}>
              {selected.has(u.userId) && '✓'}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
