import { useState } from 'react';
import { authApi } from '../api/auth';
import { useAuthStore } from '../store/authStore';
import { tokenStorage } from '../utils/tokenStorage';
import { showToast } from './Toast';
import './LoginModal.css';

interface Props {
  visible: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

/**
 * 全局登录弹窗 —— 游客点击需要登录的功能时弹出。
 * 验证码登录暂未开放:如需恢复,可参考 git 历史中 mode='sms' 相关的状态与表单代码
 */
export default function LoginModal({ visible, onClose, onSuccess }: Props) {
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const setAuth = useAuthStore((s) => s.setAuth);

  const handleLogin = async () => {
    if (!phone) return;
    if (!password.trim()) { showToast('请输入密码'); return; }
    try {
      const res = await authApi.login({ phone, loginType: 'password', password });
      const { accessToken, refreshToken } = res.data.data;
      tokenStorage.set(accessToken, refreshToken);
      const meRes = await authApi.getMe();
      setAuth(meRes.data.data, accessToken);
      showToast('登录成功');
      onClose();
      onSuccess?.();
    } catch (e: any) { showToast(e?.message || '登录失败，请检查信息'); }
  };

  if (!visible) return null;

  return (
    <div className="login-modal-overlay" onClick={onClose}>
      <div className="login-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="login-modal-header">
          <h3>登录 Finding</h3>
          <p>登录后才能使用完整功能</p>
          <button className="modal-close-btn" onClick={onClose}>✕</button>
        </div>

        <input className="input-sm" type="tel" placeholder="手机号" value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={11} />
        <input className="input-sm" type="password" placeholder="密码" value={password} onChange={(e) => setPassword(e.target.value)} />
        <button className="login-submit-sm" onClick={handleLogin}>登录</button>
      </div>
    </div>
  );
}
