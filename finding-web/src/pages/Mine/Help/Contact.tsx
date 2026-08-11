import { useNavigate } from 'react-router-dom';
import { showToast } from '../../../components/Toast';
import AppIcon, { type AppIconName } from '../../../components/AppIcon';
import '../subpage.css';
import './help.css';

const CONTACTS: { label: string; value: string; icon: AppIconName; copyable?: boolean }[] = [
  { label: '官方邮箱', value: 'service@finding.edu.cn', icon: 'mail', copyable: true },
  { label: '客服 QQ', value: '1008610086', icon: 'message', copyable: true },
  { label: '微信公众号', value: 'Finding 校园', icon: 'sparkles' },
  { label: '服务时间', value: '每天 9:00 - 21:00', icon: 'clock' },
];

export default function ContactPage() {
  const navigate = useNavigate();

  const copy = (v: string) => {
    try {
      navigator.clipboard.writeText(v);
      showToast('已复制');
    } catch {
      showToast('复制失败，请手动复制');
    }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine/help')}>←</button>
        <h2>联系客服</h2>
      </div>
      <div className="menu-list">
        {CONTACTS.map((c) => (
          <div key={c.label} className="menu-list-item" onClick={() => c.copyable && copy(c.value)}>
            <span><AppIcon name={c.icon} size={18} /></span>
            <span style={{ color: '#888', fontSize: 14 }}>{c.label}</span>
            <span style={{ marginLeft: 'auto', color: '#333' }}>{c.value}</span>
            {c.copyable && <span style={{ fontSize: 12, color: '#29241f' }}>复制</span>}
          </div>
        ))}
      </div>
      <p style={{ textAlign: 'center', fontSize: 12, color: '#bbb', padding: '16px 0' }}>
        点按可复制邮箱 / QQ 号
      </p>
    </div>
  );
}
