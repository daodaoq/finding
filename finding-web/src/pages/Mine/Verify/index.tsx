import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../../api/auth';
import { uploadApi } from '../../../api/upload';
import { useAuthStore } from '../../../store/authStore';
import { showToast } from '../../../components/Toast';
import { APP_CONFIG } from '../../../utils/config';
import './index.css';

export default function VerifyPage() {
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [realName, setRealName] = useState('');
  const [studentId, setStudentId] = useState('');
  const [school, setSchool] = useState(user?.school || APP_CONFIG.SCHOOL_NAME);
  const [studentCard, setStudentCard] = useState('');
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // 当前认证状态
  const verifiedStatus = user?.realNameVerified ?? 0;

  /** 上传学生证照片 */
  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const res = await uploadApi.uploadImage(file);
      setStudentCard(res.data.data);
    } catch {
      showToast('上传失败');
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  /** 提交认证 */
  const handleSubmit = async () => {
    if (!realName.trim()) { showToast('请输入真实姓名'); return; }
    if (!studentId.trim()) { showToast('请输入学号'); return; }
    setSubmitting(true);
    try {
      await authApi.submitVerification({
        realName: realName.trim(),
        studentId: studentId.trim(),
        school: school.trim() || APP_CONFIG.SCHOOL_NAME,
        studentCard: studentCard || undefined,
      } as any);
      // 更新本地状态为"审核中"
      if (user) setUser({ ...user, realNameVerified: 1 });
      showToast('提交成功，请等待管理员审核');
    } catch {
      showToast('提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  // ── 已通过 ──
  if (verifiedStatus === 2) {
    return (
      <div className="ver-page">
        <div className="ver-header">
          <button className="ver-back" onClick={() => navigate(-1)}>←</button>
          <span className="ver-title">学生认证</span>
        </div>
        <div className="ver-status-card approved">
          <div className="ver-status-icon">✅</div>
          <div className="ver-status-title">认证已通过</div>
          <div className="ver-status-desc">你是{APP_CONFIG.SCHOOL_NAME}已认证学生，可使用全部功能</div>
        </div>
      </div>
    );
  }

  // ── 审核中 ──
  if (verifiedStatus === 1) {
    return (
      <div className="ver-page">
        <div className="ver-header">
          <button className="ver-back" onClick={() => navigate(-1)}>←</button>
          <span className="ver-title">学生认证</span>
        </div>
        <div className="ver-status-card pending">
          <div className="ver-status-icon">⏳</div>
          <div className="ver-status-title">审核中</div>
          <div className="ver-status-desc">你的认证申请正在审核中，请耐心等待。审核通过后即可使用全部功能。</div>
        </div>
      </div>
    );
  }

  // ── 被拒绝（可重新提交）──
  const isRejected = verifiedStatus === 3;

  return (
    <div className="ver-page">
      <div className="ver-header">
        <button className="ver-back" onClick={() => navigate(-1)}>←</button>
        <span className="ver-title">学生认证</span>
      </div>

      {isRejected && (
        <div className="ver-status-card rejected">
          <div className="ver-status-icon">❌</div>
          <div className="ver-status-title">认证未通过</div>
          <div className="ver-status-desc">你的上次认证申请未通过审核，请核对信息后重新提交</div>
        </div>
      )}

      <div className="ver-form">
        {/* 真实姓名 */}
        <div className="ver-field">
          <span className="ver-field-label">姓名</span>
          <input
            className="ver-field-input"
            type="text"
            placeholder="请输入真实姓名"
            value={realName}
            onChange={(e) => setRealName(e.target.value)}
            maxLength={20}
          />
        </div>

        {/* 学号 */}
        <div className="ver-field">
          <span className="ver-field-label">学号</span>
          <input
            className="ver-field-input"
            type="text"
            placeholder="请输入学号"
            value={studentId}
            onChange={(e) => setStudentId(e.target.value)}
            maxLength={20}
          />
        </div>

        {/* 学校 */}
        <div className="ver-field">
          <span className="ver-field-label">学校</span>
          <input
            className="ver-field-input"
            type="text"
            placeholder={APP_CONFIG.SCHOOL_NAME}
            value={school}
            onChange={(e) => setSchool(e.target.value)}
            maxLength={50}
          />
        </div>

        {/* 学生证上传 */}
        <div className="ver-upload-row">
          <div className="ver-upload-label">学生证照片（选填，可加快审核）</div>
          <div
            className="ver-upload-box"
            onClick={() => fileInputRef.current?.click()}
          >
            {studentCard ? (
              <img src={studentCard} alt="学生证" />
            ) : (
              <div className="ver-upload-placeholder">
                <span>{uploading ? '⏳' : '📷'}</span>
                {uploading ? '上传中...' : '点击上传学生证照片'}
              </div>
            )}
          </div>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            style={{ display: 'none' }}
            onChange={handleUpload}
          />
        </div>
      </div>

      <p className="ver-hint">
        💡 认证信息需与学籍信息一致，管理员将在 24 小时内审核。<br />
        审核通过后即可使用发帖、评论、搭子、私信等全部功能。
      </p>

      <button
        className="ver-submit-btn"
        onClick={handleSubmit}
        disabled={submitting || uploading}
      >
        {submitting ? '提交中...' : isRejected ? '重新提交认证' : '提交认证'}
      </button>
    </div>
  );
}
