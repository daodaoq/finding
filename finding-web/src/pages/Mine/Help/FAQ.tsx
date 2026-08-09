import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../subpage.css';
import './help.css';

const FAQS = [
  { q: '如何完成学生认证？', a: '在「我的」页点击学校名称或认证提示，填写姓名、学号、学校，可选上传学生证照片，提交后等待管理员审核。' },
  { q: '认证审核需要多久？', a: '一般 24 小时内完成。可在「我的」页点学校查看认证进度和审核意见。' },
  { q: '如何互换详细信息？', a: '在聊天页顶部点击「互换信息」按钮，对方同意后即可互相查看情感简历。' },
  { q: '如何发布搭子邀约？', a: '在「搭子」页点击右下角 + 发布邀约，选择分类并填写标题、时间、地点等信息。' },
  { q: '消息收不到怎么办？', a: '检查该聊天是否开启了「消息免打扰」，或到「设置 - 聊天通用」查看全局免打扰设置。' },
  { q: '如何修改密码？', a: '在「我的 - 设置 - 账号与安全」中修改，需验证旧密码。' },
];

export default function FAQPage() {
  const navigate = useNavigate();
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine/help')}>←</button>
        <h2>常见问题</h2>
      </div>
      <div className="faq-list">
        {FAQS.map((f, i) => (
          <div key={i} className="faq-item" onClick={() => setOpenIndex(openIndex === i ? null : i)}>
            <div className="faq-q">
              <span>Q{i + 1}.</span>
              <span>{f.q}</span>
              <span className="faq-toggle">{openIndex === i ? '−' : '+'}</span>
            </div>
            {openIndex === i && <div className="faq-a">{f.a}</div>}
          </div>
        ))}
      </div>
    </div>
  );
}
