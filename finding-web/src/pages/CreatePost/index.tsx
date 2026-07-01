import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postApi } from '../../api/post';
import LoginModal from '../../components/LoginModal';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { showToast } from '../../components/Toast';
import './index.css';

export default function CreatePostPage() {
  const [content, setContent] = useState('');
  const [location, setLocation] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const { showLogin, requireLogin, handleLoginSuccess, handleClose, isLoggedIn } = useRequireLogin();

  const handleSubmit = () => {
    requireLogin(async () => {
      if (!content.trim()) { showToast('请输入内容'); return; }
      setSubmitting(true);
      try {
        await postApi.create({
          content: content.trim(),
          location: location.trim() || undefined,
        });
        showToast('发布成功！');
        navigate(-1);
      } catch { showToast('发布失败，请稍后重试'); }
      finally { setSubmitting(false); }
    });
  };

  return (
    <div className="cp-page">
      <div className="cp-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h3>发帖子</h3>
        <button className="cp-submit-btn" onClick={handleSubmit} disabled={submitting || !content.trim()}>
          {submitting ? '发布中...' : '发布'}
        </button>
      </div>

      <div className="cp-form">
        <textarea
          className="cp-textarea"
          placeholder="分享你的校园生活..."
          value={content}
          onChange={e => setContent(e.target.value)}
          maxLength={5000}
          autoFocus
        />
        <div className="cp-char-count">{content.length}/5000</div>

        <div className="cp-location-row">
          <span>📍</span>
          <input
            className="cp-location-input"
            type="text"
            placeholder="添加位置（选填）"
            value={location}
            onChange={e => setLocation(e.target.value)}
          />
        </div>
      </div>

      <div className="cp-tip">
        发布即表示同意遵守平台规范，共同维护良好的校园社区氛围。
      </div>

      {!isLoggedIn && (
        <div className="cp-login-hint" onClick={() => requireLogin(() => {})}>
          🔒 登录后即可发布动态
        </div>
      )}

      <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />
    </div>
  );
}
