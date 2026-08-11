import { useNavigate } from 'react-router-dom';
import '../subpage.css';

const ITEMS = [
  { label: '个人资料', desc: '头像、昵称、学校、签名', to: '/mine/profile' },
  { label: '账号与安全', desc: '手机号、修改密码', to: '/mine/account' },
  { label: '聊天通用', desc: '全局聊天背景、消息免打扰', to: '/mine/settings/chat' },
  { label: '加好友方式', desc: '谁可以申请加你为好友', to: '/mine/settings/friend' },
  { label: '个人权限', desc: '搜索可见性、主页可见性', to: '/mine/settings/privacy' },
  { label: '相亲偏好', desc: '偏好性别、年龄、距离、只看认证', to: '/mine/settings/preference' },
];

export default function SettingsPage() {
  const navigate = useNavigate();
  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>设置</h2>
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
