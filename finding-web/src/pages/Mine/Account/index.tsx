import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../../api/auth';
import { useAuthStore } from '../../../store/authStore';
import { showToast } from '../../../components/Toast';
import '../subpage.css';
import '../Settings/settings.css';

export default function AccountPage() {
  const navigate = useNavigate();
  const logout = useAuthStore((s) => s.logout);
  const [phone, setPhone] = useState('');
  const [oldPwd, setOldPwd] = useState('');
  const [newPwd, setNewPwd] = useState('');
  const [confirmPwd, setConfirmPwd] = useState('');
  const [loading, setLoading] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
  const [deletePwd, setDeletePwd] = useState('');
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    authApi.getAccount().then((res) => setPhone(res.data.data?.phone || '')).catch(() => {});
  }, []);

  /** 注销账号:密码二次确认 → 后端匿名化+停用 → 清本地登录态 */
  const confirmDelete = async () => {
    if (!deletePwd) { showToast('请输入密码以确认注销'); return; }
    setDeleting(true);
    try {
      await authApi.deleteAccount(deletePwd);
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      useAuthStore.getState().logout();
      navigate('/login', { replace: true });
    } catch (e: any) {
      showToast(e?.message || '注销失败');
    } finally {
      setDeleting(false);
    }
  };

  const maskPhone = (p: string) => (p.length === 11 ? `${p.slice(0, 3)}****${p.slice(7)}` : p);

  const submit = async () => {
    if (!oldPwd) { showToast('请输入旧密码'); return; }
    if (!newPwd || newPwd.length < 6) { showToast('新密码至少 6 位'); return; }
    if (newPwd !== confirmPwd) { showToast('两次输入的新密码不一致'); return; }
    setLoading(true);
    try {
      await authApi.changePassword(oldPwd, newPwd);
      showToast('密码修改成功，请重新登录');
      logout();
      navigate('/login');
    } catch (e: any) {
      showToast(e?.message || '修改失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine/settings')}>←</button>
        <h2>账号与安全</h2>
      </div>

      <div className="set-card">
        <div className="set-row">
          <span className="set-label">手机号</span>
          <span className="set-value">{maskPhone(phone) || '-'}</span>
        </div>
      </div>

      <div className="set-card">
        <div className="set-row">
          <span className="set-label">旧密码</span>
          <input className="acct-input" type="password" placeholder="请输入旧密码"
            value={oldPwd} onChange={(e) => setOldPwd(e.target.value)} />
        </div>
        <div className="set-row">
          <span className="set-label">新密码</span>
          <input className="acct-input" type="password" placeholder="至少 6 位"
            value={newPwd} onChange={(e) => setNewPwd(e.target.value)} />
        </div>
        <div className="set-row">
          <span className="set-label">确认新密码</span>
          <input className="acct-input" type="password" placeholder="再次输入新密码"
            value={confirmPwd} onChange={(e) => setConfirmPwd(e.target.value)} />
        </div>
      </div>
      <p className="set-hint">修改密码后需要重新登录</p>

      <button className="acct-submit" onClick={submit} disabled={loading}>
        {loading ? '提交中...' : '确认修改密码'}
      </button>

      <button className="acct-logout" onClick={() => { logout(); navigate('/login'); }}>
        退出登录
      </button>

      <div className="set-card acct-danger-card">
        <div className="set-row">
          <span className="set-label">注销账号</span>
          <button className="acct-danger" onClick={() => setShowDelete(true)}>注销账号</button>
        </div>
        <p className="set-hint">注销后账号资料将被匿名化且不可恢复，请谨慎操作</p>
      </div>

      {/* 注销确认弹层 */}
      {showDelete && (
        <div className="del-overlay" onClick={() => setShowDelete(false)}>
          <div className="del-card" onClick={(e) => e.stopPropagation()}>
            <h4>确认注销账号？</h4>
            <p>
              注销后：无法再登录、个人资料被匿名化（你发布的公开内容作者将显示「已注销用户」）、
              已发出的聊天记录保留但你的昵称/头像消失。此操作<b>不可恢复</b>。
            </p>
            <input
              type="password"
              placeholder="请输入登录密码确认"
              value={deletePwd}
              onChange={(e) => setDeletePwd(e.target.value)}
              className="acct-input"
            />
            <div className="del-buttons">
              <button className="del-cancel" onClick={() => { setShowDelete(false); setDeletePwd(''); }}>取消</button>
              <button className="del-confirm" onClick={confirmDelete} disabled={deleting}>
                {deleting ? '注销中...' : '确认注销'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
