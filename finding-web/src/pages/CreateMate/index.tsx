import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { mateApi } from '../../api/mate';
import LoginModal from '../../components/LoginModal';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { MATE_CATEGORIES } from '../../utils/constants';
import { showToast } from '../../components/Toast';
import AppIcon from '../../components/AppIcon';
import './index.css';

export default function CreateMatePage() {
  const [category, setCategory] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [activityTime, setActivityTime] = useState('');
  const [location, setLocation] = useState('');
  const [maxParticipants, setMaxParticipants] = useState(10);
  const [isAnonymous, setIsAnonymous] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const { showLogin, requireLogin, handleLoginSuccess, handleClose, isLoggedIn } = useRequireLogin();
  const { id } = useParams<{ id: string }>();
  const editingId = id ? Number(id) : null;

  useEffect(() => {
    if (!editingId) return;
    mateApi.detail(editingId).then(({ data }) => {
      const m = data.data;
      setCategory(m.category); setTitle(m.title); setDescription(m.description || '');
      setActivityTime(m.activityTime ? new Date(m.activityTime).toISOString().slice(0, 16) : '');
      setLocation(m.location || ''); setMaxParticipants(m.maxParticipants); setIsAnonymous(m.isAnonymous || 0);
    }).catch(() => showToast('活动不存在或无法编辑'));
  }, [editingId]);

  const handleSubmit = () => {
    requireLogin(async () => {
      if (!category) { showToast('请选择搭子分类'); return; }
      if (!title.trim()) { showToast('请输入标题'); return; }
      if (!activityTime) { showToast('请选择活动时间'); return; }
      if (!location.trim()) { showToast('请输入活动地点'); return; }
      setSubmitting(true);
      try {
        const payload = {
          category, title: title.trim(),
          description: description.trim(),
          activityTime: new Date(activityTime).toISOString(),
          location: location.trim(),
          maxParticipants,
          isAnonymous,
        };
        if (editingId) await mateApi.update(editingId, payload); else await mateApi.create(payload);
        showToast(editingId ? '活动已更新' : '发布成功！');
        navigate(-1);
      } catch { showToast('发布失败'); }
      finally { setSubmitting(false); }
    });
  };

  return (
    <div className="cm-page">
      <div className="cm-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h3>{editingId ? '编辑搭子' : '找搭子'}</h3>
        <button className="cm-submit-btn" onClick={handleSubmit} disabled={submitting}>
          {submitting ? '保存中...' : editingId ? '保存' : '发布'}
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
                <AppIcon name={cat.icon} size={16} />
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
          <span className="cm-field-icon"><AppIcon name="clock" size={18} /></span>
          <input
            className="cm-field-input"
            type="datetime-local"
            value={activityTime}
            onChange={e => setActivityTime(e.target.value)}
          />
        </div>

        {/* 地点 */}
        <div className="cm-field">
          <span className="cm-field-icon"><AppIcon name="location" size={18} /></span>
          <input
            className="cm-field-input"
            type="text"
            placeholder="活动地点 *"
            value={location}
            onChange={e => setLocation(e.target.value)}
          />
        </div>

        <label className="cm-field" style={{ justifyContent: 'space-between' }}>
          <span>匿名发布</span>
          <input type="checkbox" checked={isAnonymous === 1} onChange={e => setIsAnonymous(e.target.checked ? 1 : 0)} />
        </label>

        {/* 人数 */}
        <div className="cm-field">
          <span className="cm-field-icon"><AppIcon name="users" size={18} /></span>
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
          <AppIcon name="lock" size={14} /> 登录后即可发布搭子邀约
        </div>
      )}

      <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />
    </div>
  );
}
