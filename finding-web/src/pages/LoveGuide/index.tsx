import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { loveGuideApi, type LoveGuide } from '../../api/loveGuide';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import LoginModal from '../../components/LoginModal';
import { showToast } from '../../components/Toast';
import './index.css';

export default function LoveGuidePage() {
  const navigate = useNavigate(); const { showLogin, requireLogin, handleClose, handleLoginSuccess } = useRequireLogin();
  const [guides, setGuides] = useState<LoveGuide[]>([]); const [open, setOpen] = useState<LoveGuide | null>(null);
  const [writing, setWriting] = useState(false); const [form, setForm] = useState({ title: '', subtitle: '', content: '', category: '聊天技巧' }); const [saving, setSaving] = useState(false);
  useEffect(() => { loveGuideApi.list().then(r => setGuides(r.data.data.records || [])).catch(() => {}); }, []);
  const submit = () => requireLogin(async () => {
    if (!form.title.trim() || !form.subtitle.trim() || !form.content.trim()) return showToast('请完整填写标题、副标题和正文');
    setSaving(true); try { await loveGuideApi.create({ ...form, title: form.title.trim(), subtitle: form.subtitle.trim(), content: form.content.trim() }); setWriting(false); setForm({ title: '', subtitle: '', content: '', category: '聊天技巧' }); showToast('投稿已提交，管理员审核通过后会展示'); } catch { showToast('提交失败，请稍后重试'); } finally { setSaving(false); }
  });
  return <main className="love-guide-page">
    <header className="lg-header"><button onClick={() => navigate(-1)} aria-label="返回">‹</button><h1>恋爱干货分享</h1><span /></header>
    <section className="lg-hero"><div className="lg-plane">✈</div><h2>不会聊天？恋爱话术免费学</h2><p>校园恋爱指南｜聊天、约会、情绪相处</p></section>
    <div className="lg-tags">{['聊天话术','约会攻略','情绪沟通','避雷指南','相处技巧'].map(x => <span key={x}>{x}</span>)}</div>
    <section className="lg-list">{guides.map(g => <button className="lg-card" key={g.id} onClick={() => setOpen(g)}><h2>{g.title}</h2><p>{g.subtitle}</p><span>♥ {g.category}</span></button>)}</section>
    <button className="lg-share" onClick={() => setWriting(true)}>分享经验</button>
    {(open || writing) && <div className="lg-overlay" onClick={() => { setOpen(null); setWriting(false); }}><section className="lg-modal" onClick={e => e.stopPropagation()}>
      {open ? <><button className="lg-close" onClick={() => setOpen(null)}>×</button><span className="lg-modal-tag">♥ {open.category}</span><h2>{open.title}</h2><p className="lg-modal-subtitle">{open.subtitle}</p><article>{open.content}</article></> : <>
        <div className="lg-modal-head"><h2>分享恋爱经验</h2><button className="lg-close" onClick={() => setWriting(false)}>×</button></div><p className="lg-review-note">提交后将进入管理员审核，通过后公开展示。</p>
        <input value={form.title} maxLength={60} placeholder="标题" onChange={e => setForm({ ...form, title: e.target.value })} /><input value={form.subtitle} maxLength={100} placeholder="副标题" onChange={e => setForm({ ...form, subtitle: e.target.value })} />
        <select value={form.category} onChange={e => setForm({ ...form, category: e.target.value })}>{['聊天技巧','约会攻略','情绪沟通','避雷指南','相处技巧'].map(x => <option key={x}>{x}</option>)}</select><textarea value={form.content} maxLength={5000} placeholder="写下你的经验与建议…" onChange={e => setForm({ ...form, content: e.target.value })} /><button className="lg-submit" disabled={saving} onClick={submit}>{saving ? '提交中…' : '提交审核'}</button>
      </>}
    </section></div>}
    <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />
  </main>;
}
