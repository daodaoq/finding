import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { showToast } from '../components/Toast';
import './Login.css';

export default function RegisterPage() {
  const [phone, setPhone] = useState('');
  const [captchaKey, setCaptchaKey] = useState('');
  const [captchaImage, setCaptchaImage] = useState('');
  const [captchaCode, setCaptchaCode] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [school, setSchool] = useState('');
  const navigate = useNavigate();

  // 加载图片验证码
  const loadCaptcha = async () => {
    try {
      const res = await authApi.getCaptcha();
      setCaptchaKey(res.data.data.captchaKey);
      setCaptchaImage(res.data.data.captchaImage);
      setCaptchaCode('');
    } catch {
      showToast('验证码加载失败，请刷新');
    }
  };

  useEffect(() => { loadCaptcha(); }, []);

  const handleRegister = async () => {
    if (!phone || !captchaCode || !password || !nickname) {
      showToast('请填写所有必填项'); return;
    }
    try {
      await authApi.register({ phone, captchaKey, captchaCode, password, nickname, school });
      showToast('注册成功，请登录');
      navigate('/login');
    } catch (e: any) {
      showToast(e?.message || '注册失败');
      loadCaptcha(); // 失败后刷新验证码
    }
  };

  return (
    <div className="login-card">
      <button className="login-back-btn" onClick={() => navigate(-1)}>← 返回</button>
      <h2 style={{ textAlign: 'center', marginBottom: 20, color: '#333' }}>注册账号</h2>
      <input className="input" type="tel" placeholder="手机号 *" value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={11} />
      <div className="sms-row">
        <input className="input sms-input" type="text" placeholder="图片验证码 *" value={captchaCode} onChange={(e) => setCaptchaCode(e.target.value)} maxLength={4} />
        <img
          className="captcha-img"
          src={`data:image/png;base64,${captchaImage}`}
          alt="验证码"
          title="看不清？点击换一张"
          onClick={loadCaptcha}
        />
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
