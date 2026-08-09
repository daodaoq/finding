import { useNavigate } from 'react-router-dom';
import '../subpage.css';
import './help.css';

const SECTIONS = [
  { title: '广场', text: '发布动态、浏览同学分享的日常。点右下角 + 可发布图文动态，支持点赞、评论、关注。' },
  { title: '鹊桥', text: '校园交友推荐页，系统按学校、城市、兴趣为你匹配心动对象。点「申请」发送聊天申请，对方同意后即可私聊。' },
  { title: '搭子', text: '找学习、运动、游戏、拼车等搭子。发布邀约或申请加入，对方审核通过后即可组队。' },
  { title: '情感简历', text: '在「我的 - 情感简历」填写个人资料、自我画像、恋爱观等。与对方在聊天中互换信息后可互相查看。' },
  { title: '私信与群聊', text: '聊天支持文字、图片，可置顶、免打扰、设置聊天背景。群聊可邀请好友加入。' },
  { title: '学生认证', text: '完成学生认证后可解锁发帖、评论、私信、搭子等全部功能。认证信息仅管理员可见。' },
];

export default function GuidePage() {
  const navigate = useNavigate();
  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine/help')}>←</button>
        <h2>功能了解与使用</h2>
      </div>
      <div style={{ paddingBottom: 12 }}>
        {SECTIONS.map((s) => (
          <div key={s.title} className="guide-section">
            <h3>{s.title}</h3>
            <p>{s.text}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
