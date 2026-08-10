import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { showToast } from '../../../components/Toast';
import request from '../../../api/request';
import '../subpage.css';
import './help.css';

export default function HumanPage() {
  const navigate = useNavigate();
  const [desc, setDesc] = useState('');
  const [contact, setContact] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async () => {
    if (!desc.trim()) { showToast('请描述你遇到的问题'); return; }
    setSubmitting(true);
    try {
      await request.post('/feedback', {
        type: 'other',
        content: desc.trim(),
        contact: contact.trim() || undefined,
      });
      showToast('已提交，客服会尽快联系你');
      setDesc('');
      setContact('');
    } catch (e: any) {
      showToast(e?.message || '提交失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine/help')}>←</button>
        <h2>转人工</h2>
      </div>
      <div className="human-form">
        <p style={{ margin: 0, fontSize: 13, color: '#888' }}>
          常见问题没能解决？留下问题描述和联系方式，人工客服将尽快联系你。
        </p>
        <textarea
          rows={4}
          placeholder="请描述你遇到的问题（必填）"
          value={desc}
          onChange={(e) => setDesc(e.target.value)}
        />
        <input
          placeholder="手机号 / 微信号（选填）"
          value={contact}
          onChange={(e) => setContact(e.target.value)}
        />
        <button className="human-submit" onClick={submit} disabled={submitting}>
          {submitting ? '提交中...' : '提交给客服'}
        </button>
      </div>
    </div>
  );
}
