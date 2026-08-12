import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { showToast } from '../components/Toast';
import SliderCaptcha from '../components/SliderCaptcha';
import './Login.css';

export default function RegisterPage() {
  const [phone, setPhone] = useState('');
  const [captchaKey, setCaptchaKey] = useState('');
  const [bgImage, setBgImage] = useState('');
  const [pieceImage, setPieceImage] = useState('');
  const [captchaY, setCaptchaY] = useState(0);
  const [captchaX, setCaptchaX] = useState<number | null>(null);
  const [captchaTime, setCaptchaTime] = useState(0);
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [school, setSchool] = useState('');
  const navigate = useNavigate();

  // 加载滑块拼图验证码
  const loadCaptcha = async () => {
    try {
      const res = await authApi.getCaptcha();
      setCaptchaKey(res.data.data.captchaKey);
      setBgImage(res.data.data.bgImage);
      setPieceImage(res.data.data.pieceImage);
      setCaptchaY(Number(res.data.data.y) || 0);
      setCaptchaX(null);
      setCaptchaTime(0);
    } catch {
      showToast('验证码加载失败，请刷新');
    }
  };

  useEffect(() => { loadCaptcha(); }, []);

  const handleRegister = async () => {
    if (!phone || !password || !nickname) {
      showToast('请填写所有必填项'); return;
    }
    if (captchaX == null) {
      showToast('请先拖动滑块完成验证'); return;
    }
    try {
      await authApi.register({
        phone, captchaKey, captchaX, captchaTime, password, nickname, school,
      });
      showToast('注册成功，请登录');
      navigate('/login');
    } catch (e: any) {
      showToast(e?.message || '注册失败');
      loadCaptcha(); // 失败后刷新验证码(一次性,已消耗)
    }
  };

  return (
    <div className="login-card">
      <button className="login-back-btn" onClick={() => navigate(-1)}>← 返回</button>
      <h2 style={{ textAlign: 'center', marginBottom: 20, color: '#333' }}>注册账号</h2>
      <input className="input" type="tel" placeholder="手机号 *" value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={11} />
      {captchaKey && (
        <SliderCaptcha
          key={captchaKey}
          bg={bgImage}
          piece={pieceImage}
          y={captchaY}
          onDone={(x, t) => { setCaptchaX(x); setCaptchaTime(t); }}
        />
      )}
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
