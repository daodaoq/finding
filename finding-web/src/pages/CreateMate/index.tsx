import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mateApi } from '../../api/mate';
import LoginModal from '../../components/LoginModal';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { MATE_CATEGORIES } from '../../utils/constants';
import { showToast } from '../../components/Toast';
import './index.css';

export default function CreateMatePage() {
  const [category, setCategory] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [activityTime, setActivityTime] = useState('');
  const [location, setLocation] = useState('');
  const [maxParticipants, setMaxParticipants] = useState(10);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const { showLogin, requireLogin, handleLoginSuccess, handleClose, isLoggedIn } = useRequireLogin();

  const handleSubmit = () => {
    requireLogin(async () => {
      if (!category) { showToast('请选择搭子分类'); return; }
      if (!title.trim()) { showToast('请输入标题'); return; }
      if (!activityTime) { showToast('请选择活动时间'); return; }
      setSubmitting(true);
      try {
        await mateApi.create({
          category, title: title.trim(),
          description: description.trim(),
          activityTime: activityTime ? new Date(activityTime).toISOString() : undefined,
          location: location.trim() || undefined,
          maxParticipants,
        });
        showToast('发布成功！');
        navigate(-1);
      } catch { showToast('发布失败'); }
      finally { setSubmitting(false); }
    });
  };

  return (
    <div className="cm-page">
      <div className="cm-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h3>找搭子</h3>
        <button className="cm-submit-btn" onClick={handleSubmit} disabled={submitting}>
          {submitting ? '发布中...' : '发布'}
        </button>
      </div>

      <div className="cm-form">
        {/* 分类选择 */}
        <div className="cm-section">
          <label className="cm-label">搭子分类 *</label>
          <div className="cm-category-grid">
            {MATE_CATEGORIES.map(cat => (
              <button
                key={cat.code}
                className={`cm-cat-cell ${category === cat.code ? 'active' : ''}`}
                onClick={() => setCategory(category === cat.code ? '' : cat.code)}
              >
                <span>{cat.icon}</span>
                <span>{cat.name}</span>
              </button>
            ))}
          </div>
        </div>

        {/* 标题 */}
        <input
          className="cm-input"
          type="text"
          placeholder="活动标题 *（如：周六下午踢球）"
          value={title}
          onChange={e => setTitle(e.target.value)}
          maxLength={100}
        />

        {/* 描述 */}
        <textarea
          className="cm-textarea"
          placeholder="活动描述（选填，如：缺5个人，来的报名）"
          value={description}
          onChange={e => setDescription(e.target.value)}
          rows={3}
        />

        {/* 时间 */}
        <div className="cm-field">
          <span className="cm-field-icon">🕐</span>
          <input
            className="cm-field-input"
            type="datetime-local"
            value={activityTime}
            onChange={e => setActivityTime(e.target.value)}
          />
        </div>

        {/* 地点 */}
        <div className="cm-field">
          <span className="cm-field-icon">📍</span>
          <input
            className="cm-field-input"
            type="text"
            placeholder="活动地点（选填）"
            value={location}
            onChange={e => setLocation(e.target.value)}
          />
        </div>

        {/* 人数 */}
        <div className="cm-field">
          <span className="cm-field-icon">👥</span>
          <select
            className="cm-field-input"
            value={maxParticipants}
            onChange={e => setMaxParticipants(Number(e.target.value))}
          >
            {[2,3,4,5,6,8,10,15,20].map(n => (
              <option key={n} value={n}>最多 {n} 人</option>
            ))}
          </select>
        </div>
      </div>

      {!isLoggedIn && (
        <div className="cm-login-hint" onClick={() => requireLogin(() => {})}>
          🔒 登录后即可发布搭子邀约
        </div>
      )}

      <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />
    </div>
  );
}
