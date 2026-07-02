import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { useAuthStore } from '../store/authStore';
import { showToast } from '../components/Toast';
import './Login.css';

export default function LoginPage() {
  const [phone, setPhone] = useState('');
  const [mode, setMode] = useState<'password' | 'sms'>('password');
  const [password, setPassword] = useState('');
  const [smsCode, setSmsCode] = useState('');
  const [sending, setSending] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const handleSendCode = async () => {
    if (!/^1[3-9]\d{9}$/.test(phone)) { showToast('请输入正确的手机号'); return; }
    try {
      setSending(true);
      await authApi.sendCode(phone, 'login');
      showToast('验证码已发送');
      setCountdown(60);
      const t = setInterval(() => setCountdown((c) => { if (c <= 1) { clearInterval(t); return 0; } return c - 1; }), 1000);
    } catch { showToast('发送失败，请稍后再试'); }
    finally { setSending(false); }
  };

  const handleLogin = async () => {
    if (!phone) { showToast('请输入手机号'); return; }
    try {
      const res = await authApi.login({
        phone, loginType: mode,
        ...(mode === 'password' ? { password } : { smsCode }),
      });
      const { accessToken, refreshToken } = res.data.data;
      // 先存储 token，后续请求才能带上 Authorization 头
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      // 获取用户信息
      const meRes = await authApi.getMe();
      setAuth(meRes.data.data, accessToken);
      showToast('登录成功');
      navigate('/');
    } catch (e: any) { showToast(e?.message || '登录失败，请检查信息'); }
  };

  return (
    <div className="login-card">
      <button className="login-back-btn" onClick={() => navigate(-1)}>← 返回</button>
      <div className="login-tabs">
        <button className={mode === 'password' ? 'active' : ''} onClick={() => setMode('password')}>密码登录</button>
        <button className={mode === 'sms' ? 'active' : ''} onClick={() => setMode('sms')}>验证码登录</button>
      </div>
      <input className="input" type="tel" placeholder="手机号" value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={11} />
      {mode === 'password' ? (
        <input className="input" type="password" placeholder="密码" value={password} onChange={(e) => setPassword(e.target.value)} />
      ) : (
        <div className="sms-row">
          <input className="input sms-input" type="text" placeholder="验证码" value={smsCode} onChange={(e) => setSmsCode(e.target.value)} maxLength={6} />
          <button className="sms-btn" onClick={handleSendCode} disabled={sending || countdown > 0}>
            {countdown > 0 ? `${countdown}s` : '获取验证码'}
          </button>
        </div>
      )}
      <button className="submit-btn" onClick={handleLogin}>登录</button>
      <div className="login-footer">
        <span>还没有账号？</span><Link to="/register">立即注册</Link>
      </div>
    </div>
  );
}
