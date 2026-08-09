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

  useEffect(() => {
    authApi.getAccount().then((res) => setPhone(res.data.data?.phone || '')).catch(() => {});
  }, []);

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
    </div>
  );
}
