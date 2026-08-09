import { useNavigate } from 'react-router-dom';
import '../subpage.css';

const ITEMS = [
  { label: '常见问题', desc: '账号、认证、功能常见疑问', to: '/mine/help/faq' },
  { label: '联系客服', desc: '客服联系方式', to: '/mine/help/contact' },
  { label: '功能了解与使用', desc: '功能介绍与使用说明', to: '/mine/help/guide' },
  { label: '转人工', desc: '人工服务入口', to: '/mine/help/human' },
];

export default function HelpPage() {
  const navigate = useNavigate();
  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>帮助与客服</h2>
      </div>
      <div className="menu-list">
        {ITEMS.map((i) => (
          <div key={i.label} className="menu-list-item" onClick={() => navigate(i.to)}>
            <span>{i.label}</span>
            <span style={{ fontSize: 12, color: '#bbb' }}>{i.desc}</span>
            <span className="menu-list-arrow">›</span>
          </div>
        ))}
      </div>
    </div>
  );
}
