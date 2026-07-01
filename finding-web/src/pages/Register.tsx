import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { showToast } from '../components/Toast';
import './Login.css';

export default function RegisterPage() {
  const [phone, setPhone] = useState('');
  const [smsCode, setSmsCode] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [school, setSchool] = useState('');
  const [countdown, setCountdown] = useState(0);
  const navigate = useNavigate();

  const handleSendCode = async () => {
    if (!/^1[3-9]\d{9}$/.test(phone)) { showToast('请输入正确的手机号'); return; }
    try {
      await authApi.sendCode(phone, 'register');
      showToast('验证码已发送');
      setCountdown(60);
      const t = setInterval(() => setCountdown((c) => { if (c <= 1) { clearInterval(t); return 0; } return c - 1; }), 1000);
    } catch { showToast('发送失败'); }
  };

  const handleRegister = async () => {
    if (!phone || !smsCode || !password || !nickname) {
      showToast('请填写所有必填项'); return;
    }
    try {
      await authApi.register({ phone, smsCode, password, nickname, school });
      showToast('注册成功，请登录');
      navigate('/login');
    } catch { showToast('注册失败'); }
  };

  return (
    <div className="login-card">
      <h2 style={{ textAlign: 'center', marginBottom: 20, color: '#333' }}>注册账号</h2>
      <input className="input" type="tel" placeholder="手机号 *" value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={11} />
      <div className="sms-row">
        <input className="input sms-input" type="text" placeholder="验证码 *" value={smsCode} onChange={(e) => setSmsCode(e.target.value)} maxLength={6} />
        <button className="sms-btn" onClick={handleSendCode} disabled={countdown > 0}>
          {countdown > 0 ? `${countdown}s` : '获取验证码'}
        </button>
      </div>
      <input className="input" type="password" placeholder="密码 (8-32位) *" value={password} onChange={(e) => setPassword(e.target.value)} />
      <input className="input" type="text" placeholder="昵称 *" value={nickname} onChange={(e) => setNickname(e.target.value)} />
      <input className="input" type="text" placeholder="学校 (选填)" value={school} onChange={(e) => setSchool(e.target.value)} />
      <button className="submit-btn" onClick={handleRegister}>注册</button>
      <div className="login-footer">
        <span>已有账号？</span><Link to="/login">立即登录</Link>
      </div>
    </div>
  );
}
