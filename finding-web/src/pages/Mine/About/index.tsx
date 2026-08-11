import { useNavigate } from 'react-router-dom';
import { APP_CONFIG } from '../../../utils/config';
import AppIcon from '../../../components/AppIcon';
import '../subpage.css';
import '../Settings/settings.css';

const CHANGELOG = [
  { version: 'v1.2.0', date: '2026-08', items: ['全新「我的」页布局', '新增浏览记录', '新增订单/帮助与客服/设置/关于我们', '全局聊天设置(可被单聊覆盖)', '加好友方式与个人权限设置'] },
  { version: 'v1.1.0', date: '2026-07', items: ['情感简历与信息互换', '聊天信息页(置顶/免打扰/背景/清空/投诉)', '图片验证码注册'] },
  { version: 'v1.0.0', date: '2026-06', items: ['学生认证', '广场动态/评论/点赞', '鹊桥交友与聊天申请', '搭子邀约', '私信/群聊'] },
];

export default function AboutPage() {
  const navigate = useNavigate();
  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>关于我们</h2>
      </div>

      <div className="set-card" style={{ textAlign: 'center', padding: '28px 16px' }}>
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <AppIcon name="heart" size={40} color="#29241f" fill="#29241f" />
        </div>
        <div style={{ fontSize: 18, fontWeight: 700, color: '#29241f', margin: '8px 0 4px' }}>
          Finding
        </div>
        <div className="set-desc">{APP_CONFIG.SCHOOL_NAME}学生专属社交平台</div>
        <div className="set-desc" style={{ marginTop: 6 }}>
          当前版本 {APP_CONFIG.VERSION}
        </div>
      </div>

      {CHANGELOG.map((c) => (
        <div key={c.version} className="set-card" style={{ padding: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
            <span style={{ fontSize: 15, fontWeight: 600, color: '#333' }}>{c.version}</span>
            <span style={{ fontSize: 12, color: '#bbb' }}>{c.date}</span>
          </div>
          {c.items.map((item, i) => (
            <div key={i} style={{ fontSize: 13, color: '#666', lineHeight: 1.9 }}>· {item}</div>
          ))}
        </div>
      ))}

      <p className="set-hint" style={{ textAlign: 'center' }}>
        Finding ©2026 · {APP_CONFIG.SCHOOL_NAME}
      </p>
    </div>
  );
}
