import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { useAuthStore } from '../store/authStore';
import { tokenStorage } from '../utils/tokenStorage';
import { showToast } from '../components/Toast';
import './Login.css';

// 验证码登录暂未开放:如需恢复,可参考 git 历史中 mode='sms' 相关的状态与表单代码
export default function LoginPage() {
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const handleLogin = async () => {
    const p = phone.trim();
    if (!p) { showToast('请输入手机号'); return; }
    if (!password.trim()) { showToast('请输入密码'); return; }
    try {
      const res = await authApi.login({
        phone: p,
        loginType: 'password',
        password: password.trim(),
      });
      const { accessToken, refreshToken } = res.data.data;
      // 先存储 token，后续请求才能带上 Authorization 头
      tokenStorage.set(accessToken, refreshToken);
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
      <h2 className="login-title">手机号登录</h2>
      <input className="input" type="tel" placeholder="手机号" value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={11} />
      <input className="input" type="password" placeholder="密码" value={password} onChange={(e) => setPassword(e.target.value)} />
      <button className="submit-btn" onClick={handleLogin}>登录</button>
      <div className="login-footer">
        <span>还没有账号？</span><Link to="/register">立即注册</Link>
      </div>
    </div>
  );
}
