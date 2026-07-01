import { useState } from 'react';
import { authApi } from '../api/auth';
import { useAuthStore } from '../store/authStore';
import { showToast } from './Toast';
import './LoginModal.css';

interface Props {
  visible: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

/**
 * 全局登录弹窗 —— 游客点击需要登录的功能时弹出。
 */
export default function LoginModal({ visible, onClose, onSuccess }: Props) {
  const [phone, setPhone] = useState('');
  const [mode, setMode] = useState<'password' | 'sms'>('password');
  const [password, setPassword] = useState('');
  const [smsCode, setSmsCode] = useState('');
  const [countdown, setCountdown] = useState(0);
  const setAuth = useAuthStore((s) => s.setAuth);

  const handleSendCode = async () => {
    if (!/^1[3-9]\d{9}$/.test(phone)) { showToast('请输入正确的手机号'); return; }
    try {
      await authApi.sendCode(phone, 'login');
      showToast('验证码已发送');
      setCountdown(60);
      const t = setInterval(() => setCountdown((c) => { if (c <= 1) { clearInterval(t); return 0; } return c - 1; }), 1000);
    } catch { showToast('发送失败'); }
  };

  const handleLogin = async () => {
    if (!phone) return;
    try {
      const res = await authApi.login({
        phone, loginType: mode,
        ...(mode === 'password' ? { password } : { smsCode }),
      });
      const { accessToken, refreshToken } = res.data.data;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      const meRes = await authApi.getMe();
      setAuth(meRes.data.data, accessToken);
      showToast('登录成功');
      onClose();
      onSuccess?.();
    } catch { showToast('登录失败，请检查信息'); }
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

        <div className="login-tabs-sm">
          <button className={mode === 'password' ? 'active' : ''} onClick={() => setMode('password')}>密码登录</button>
          <button className={mode === 'sms' ? 'active' : ''} onClick={() => setMode('sms')}>验证码登录</button>
        </div>

        <input className="input-sm" type="tel" placeholder="手机号" value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={11} />
        {mode === 'password' ? (
          <input className="input-sm" type="password" placeholder="密码" value={password} onChange={(e) => setPassword(e.target.value)} />
        ) : (
          <div className="sms-row-sm">
            <input className="input-sm sms-input-sm" type="text" placeholder="验证码" value={smsCode} onChange={(e) => setSmsCode(e.target.value)} maxLength={6} />
            <button className="sms-btn-sm" onClick={handleSendCode} disabled={countdown > 0}>
              {countdown > 0 ? `${countdown}s` : '获取验证码'}
            </button>
          </div>
        )}
        <button className="login-submit-sm" onClick={handleLogin}>登录</button>
      </div>
    </div>
  );
}
