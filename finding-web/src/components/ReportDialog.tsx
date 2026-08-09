import { useState } from 'react';
import { reportApi } from '../api/report';
import { showToast } from './Toast';
import './ConfirmDialog.css';
import './ReportDialog.css';

const REASONS = ['骚扰 / 不文明用语', '诈骗嫌疑', '色情低俗', '冒充身份', '广告营销', '其他'];

interface Props {
  /** 被投诉类型:message/post/comment/user/resume */
  targetType: string;
  targetId: number;
  roomId?: number;
  /** 展示文案,如「该动态」「这条消息」「该用户」 */
  title: string;
  onClose: () => void;
}

/** 通用投诉弹窗 —— 预设原因 + 补充说明 */
export default function ReportDialog({ targetType, targetId, roomId, title, onClose }: Props) {
  const [reason, setReason] = useState('');
  const [custom, setCustom] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async () => {
    const finalReason = custom.trim() || reason;
    if (!finalReason) { showToast('请选择或填写投诉原因'); return; }
    setSubmitting(true);
    try {
      await reportApi.report({ targetType, targetId, reason: finalReason, roomId });
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
        <div className="confirm-buttons">
          <button className="confirm-btn cancel" onClick={onClose}>取消</button>
          <button className="confirm-btn primary" onClick={submit} disabled={submitting}>
            {submitting ? '提交中...' : '提交投诉'}
          </button>
        </div>
      </div>
    </div>
  );
}
