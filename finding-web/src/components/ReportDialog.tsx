import { useRef, useState } from 'react';
import { reportApi } from '../api/report';
import { uploadApi } from '../api/upload';
import { showToast } from './Toast';
import './ConfirmDialog.css';
import './ReportDialog.css';

const REASONS = ['骚扰', '冒充身份', '涉黄/性骚扰', '诈骗/交易引流', '泄露隐私', '辱骂', '广告营销', '其他'];
const MAX_EVIDENCE = 3;

interface Props {
  /** 被投诉类型:message/post/comment/user/resume */
  targetType: string;
  targetId: number;
  roomId?: number;
  /** 展示文案,如「该动态」「这条消息」「该用户」 */
  title: string;
  onClose: () => void;
}

/** 通用投诉弹窗 —— 预设原因 + 补充说明 + 证据图片(最多3张) */
export default function ReportDialog({ targetType, targetId, roomId, title, onClose }: Props) {
  const [reason, setReason] = useState('');
  const [custom, setCustom] = useState('');
  const [evidence, setEvidence] = useState<string[]>([]);
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (evidence.length >= MAX_EVIDENCE) { showToast(`最多上传 ${MAX_EVIDENCE} 张证据`); return; }
    setUploading(true);
    try {
      const res = await uploadApi.uploadImage(file);
      setEvidence((prev) => [...prev, res.data.data]);
    } catch { showToast('证据上传失败，请重试'); }
    finally {
      setUploading(false);
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  const submit = async () => {
    const finalReason = custom.trim() || reason;
    if (!finalReason) { showToast('请选择或填写投诉原因'); return; }
    setSubmitting(true);
    try {
      await reportApi.report({ targetType, targetId, reason: finalReason, roomId, evidence: evidence.length ? evidence : undefined });
      showToast('投诉已提交，我们会尽快处理');
      onClose();
    } catch (e: any) {
      showToast(e?.message || '提交失败，请重试');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="confirm-overlay" onClick={onClose}>
      <div className="confirm-card report-card" onClick={(e) => e.stopPropagation()}>
        <h4 className="confirm-title report-title">投诉{title}</h4>
        <div className="report-reasons">
          {REASONS.map((r) => (
            <button
              key={r}
              className={`report-reason ${reason === r ? 'active' : ''}`}
              onClick={() => { setReason(r); setCustom(''); }}
            >
              {r}
            </button>
          ))}
        </div>
        <textarea
          className="report-input"
          rows={2}
          placeholder="补充说明（选填）"
          value={custom}
          onChange={(e) => { setCustom(e.target.value); setReason(''); }}
        />
        {/* 证据上传 */}
        <div className="report-evidence">
          {evidence.map((url) => (
            <div key={url} className="report-evidence-item">
              <img src={url} alt="" />
              <button
                className="report-evidence-del"
                onClick={() => setEvidence((prev) => prev.filter((u) => u !== url))}
              >✕</button>
            </div>
          ))}
          {evidence.length < MAX_EVIDENCE && (
            <button
              className="report-evidence-add"
              onClick={() => fileRef.current?.click()}
              disabled={uploading}
            >
              {uploading ? '上传中...' : '＋ 证据截图'}
            </button>
          )}
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            style={{ display: 'none' }}
            onChange={handleFileChange}
          />
        </div>
        <div className="confirm-buttons">
          <button className="confirm-btn cancel" onClick={onClose}>取消</button>
          <button className="confirm-btn primary" onClick={submit} disabled={submitting || uploading}>
            {submitting ? '提交中...' : '提交投诉'}
          </button>
        </div>
      </div>
    </div>
  );
}
