import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../../store/authStore';
import { authApi } from '../../../api/auth';
import { uploadApi } from '../../../api/upload';
import { showToast } from '../../../components/Toast';
import { APP_CONFIG } from '../../../utils/config';
import './index.css';

const GENDER_OPTIONS = [
  { value: 0, label: '未设置' },
  { value: 1, label: '男' },
  { value: 2, label: '女' },
];

export default function ProfileEditPage() {
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [avatar, setAvatar] = useState(user?.avatar || '');
  const [nickname, setNickname] = useState(user?.nickname || '');
  const [gender, setGender] = useState(user?.gender ?? 0);
  const [school, setSchool] = useState(user?.school || '');
  const [signature, setSignature] = useState(user?.signature || '');
  const [city, setCity] = useState(user?.city || '');
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);

  /** 选择并上传头像 */
  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const res = await uploadApi.uploadImage(file);
      const url = res.data.data;
      setAvatar(url);
      showToast('头像上传成功');
    } catch {
      showToast('头像上传失败');
    } finally {
      setUploading(false);
      // 清空 input，允许重复选择同一文件
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  /** 保存个人信息 */
  const handleSave = async () => {
    if (!nickname.trim()) {
      showToast('昵称不能为空');
      return;
    }
    setSaving(true);
    try {
      await authApi.updateProfile({
        nickname: nickname.trim(),
        avatar,
        gender,
        school: school.trim() || undefined,
        signature: signature.trim() || undefined,
        city: city.trim() || undefined,
      } as any);
      // 更新本地 store
      if (user) {
        setUser({
          ...user,
          avatar,
          nickname: nickname.trim(),
          gender,
          school: school.trim() || user.school,
          signature: signature.trim() || user.signature,
          city: city.trim() || user.city,
        });
      }
      showToast('保存成功');
      navigate(-1);
    } catch {
      showToast('保存失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="pe-page">
      {/* 头部 */}
      <div className="pe-header">
        <button className="pe-back" onClick={() => navigate(-1)}>←</button>
        <span className="pe-title">编辑资料</span>
      </div>

      {/* 头像 */}
      <div className="pe-avatar-section">
        <div
          className="pe-avatar-wrapper"
          onClick={() => fileInputRef.current?.click()}
        >
          {avatar ? (
            <img src={avatar} alt="" />
          ) : (
            <span>👤</span>
          )}
          <div className="pe-avatar-overlay">更换头像</div>
          {uploading && <div className="pe-avatar-loading">上传中...</div>}
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          style={{ display: 'none' }}
          onChange={handleAvatarChange}
        />
      </div>

      {/* 表单 */}
      <div className="pe-form">
        {/* 昵称 */}
        <div className="pe-field">
          <span className="pe-field-label">昵称</span>
          <input
            className="pe-field-input"
            type="text"
            placeholder="请输入昵称"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            maxLength={20}
          />
        </div>

        {/* 性别 */}
        <div className="pe-field">
          <span className="pe-field-label">性别</span>
          <div className="pe-gender-group">
            {GENDER_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                className={`pe-gender-chip ${gender === opt.value ? 'active' : ''}`}
                onClick={() => setGender(opt.value)}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* 学校 */}
        <div className="pe-field">
          <span className="pe-field-label">学校</span>
          <input
            className="pe-field-input"
            type="text"
            placeholder={APP_CONFIG.SCHOOL_NAME}
            value={school}
            onChange={(e) => setSchool(e.target.value)}
            maxLength={50}
          />
        </div>

        {/* 个性签名 */}
        <div className="pe-field">
          <span className="pe-field-label">签名</span>
          <input
            className="pe-field-input"
            type="text"
            placeholder="介绍一下自己..."
            value={signature}
            onChange={(e) => setSignature(e.target.value)}
            maxLength={50}
          />
        </div>

        {/* 城市 */}
        <div className="pe-field">
          <span className="pe-field-label">城市</span>
          <input
            className="pe-field-input"
            type="text"
            placeholder="淄博"
            value={city}
            onChange={(e) => setCity(e.target.value)}
            maxLength={20}
          />
        </div>
      </div>

      {/* 保存 */}
      <button
        className="pe-save-btn"
        onClick={handleSave}
        disabled={saving || uploading}
      >
        {saving ? '保存中...' : '保存'}
      </button>
    </div>
  );
}
