import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { postApi } from '../../api/post';
import { uploadApi } from '../../api/upload';
import LoginModal from '../../components/LoginModal';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import { showToast } from '../../components/Toast';
import './index.css';

const MAX_IMAGES = 9;

export default function CreatePostPage() {
  const [searchParams] = useSearchParams();
  const editId = Number(searchParams.get('id')) || 0; // >0 表示编辑模式
  const [content, setContent] = useState('');
  const [location, setLocation] = useState('');
  const [images, setImages] = useState<string[]>([]);
  const [uploading, setUploading] = useState(false);
  const [loading, setLoading] = useState(!!editId);
  const [submitting, setSubmitting] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const { showLogin, requireLogin, handleLoginSuccess, handleClose, isLoggedIn } = useRequireLogin();

  // 编辑模式:加载原动态(含图片)
  useEffect(() => {
    if (!editId) return;
    postApi.detail(editId)
      .then((res) => {
        setContent(res.data.data.content);
        setLocation(res.data.data.location || '');
        setImages(res.data.data.images || []);
      })
      .catch(() => { navigate(-1); })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editId]);

  // ── 图片上传(多选,串行上传) ──
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;
    if (images.length + files.length > MAX_IMAGES) {
      showToast(`最多上传 ${MAX_IMAGES} 张图片`);
      return;
    }
    setUploading(true);
    try {
      const urls: string[] = [];
      for (const f of files) {
        const res = await uploadApi.uploadImage(f);
        urls.push(res.data.data);
      }
      setImages((prev) => [...prev, ...urls]);
    } catch {
      showToast('图片上传失败，请重试');
    } finally {
      setUploading(false);
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  const removeImage = (index: number) => {
    setImages((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = () => {
    requireLogin(async () => {
      if (!content.trim()) { showToast('请输入内容'); return; }
      setSubmitting(true);
      try {
        const payload = {
          content: content.trim(),
          images: images.length ? images : undefined,
          location: location.trim() || undefined,
        };
        if (editId) {
          await postApi.update(editId, payload);
          showToast('保存成功！');
        } else {
          await postApi.create(payload);
          showToast('发布成功！');
        }
        navigate(-1);
      } catch { showToast('操作失败，请稍后重试'); }
      finally { setSubmitting(false); }
    });
  };

  return (
    <div className="cp-page">
      <div className="cp-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h3>{editId ? '编辑动态' : '发帖子'}</h3>
        <button className="cp-submit-btn" onClick={handleSubmit} disabled={submitting || loading || !content.trim()}>
          {submitting ? '保存中...' : editId ? '保存' : '发布'}
        </button>
      </div>

      <div className="cp-form">
        {loading ? (
          <div className="cp-loading">加载中...</div>
        ) : (
          <textarea
            className="cp-textarea"
            placeholder="分享你的校园生活..."
            value={content}
            onChange={e => setContent(e.target.value)}
            maxLength={5000}
            autoFocus
          />
        )}
        <div className="cp-char-count">{content.length}/5000</div>

        {/* 图片上传区 */}
        <div className="cp-images" style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginTop: 4 }}>
          {images.map((url, i) => (
            <div key={i} style={{ position: 'relative', width: 72, height: 72 }}>
              <img src={url} alt="" style={{ width: 72, height: 72, objectFit: 'cover', borderRadius: 8 }} />
              <button
                onClick={() => removeImage(i)}
                style={{
                  position: 'absolute', top: -7, right: -7, width: 20, height: 20, borderRadius: '50%',
                  border: 'none', background: 'rgba(0,0,0,0.55)', color: '#fff', fontSize: 12, lineHeight: 1,
                  cursor: 'pointer',
                }}
              >✕</button>
            </div>
          ))}
          {images.length < MAX_IMAGES && (
            <button
              onClick={() => fileRef.current?.click()}
              style={{
                width: 72, height: 72, border: '1px dashed #ccc', borderRadius: 8, background: '#fafafa',
                fontSize: 24, color: '#999', cursor: 'pointer',
              }}
            >{uploading ? '上传中' : '＋'}</button>
          )}
          <input ref={fileRef} type="file" accept="image/*" multiple hidden onChange={handleFileChange} />
        </div>

        <div className="cp-location-row" style={{ marginTop: 12 }}>
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
