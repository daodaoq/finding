import type { UserResume } from '../types/resume';
import './ResumeView.css';

interface Props {
  resume: UserResume;
  avatar?: string;
}

/** 情感简历 —— 只读 9 卡片展示(互换后可见) */
export default function ResumeView({ resume, avatar }: Props) {
  const genderText = (g?: number) => (g === 1 ? '男' : g === 2 ? '女' : undefined);

  const basicRows: [string, any][] = [
    ['性别', genderText(resume.gender)],
    ['年龄', resume.age],
    ['生日', resume.birthday],
    ['星座', resume.constellation],
    ['身高', resume.heightCm ? `${resume.heightCm}cm` : undefined],
    ['体重', resume.weightKg ? `${resume.weightKg}kg` : undefined],
    ['城市', resume.hometown],
    ['校区', resume.campus],
    ['专业年级', resume.majorGrade],
    ['职业', resume.career],
    ['日常作息', resume.dailyRoutine],
    ['恋爱状态', resume.relationshipStatus],
    ['择偶核心底线', resume.coreBottomLine],
  ];

  return (
    <div className="resume-view">
      <Section icon="💁" title="基础信息栏" rows={basicRows}>
        <div className="resume-avatar-row">
          <div className="resume-avatar">
            {avatar ? <img src={avatar} alt="" /> : <span>👤</span>}
          </div>
          <span className="resume-avatar-label">照片</span>
        </div>
      </Section>

      <Section icon="🎨" title="自我画像" rows={[
        ['性格优点', resume.personalityTraits],
        ['小缺点', resume.flaws],
        ['个人三观', resume.worldview],
        ['个人标签', resume.personalTags],
        ['MBTI 人格', resume.mbti],
        ['恋爱中的样子', resume.inLoveLook],
      ]} />

      <Section icon="💭" title="过往恋爱复盘" rows={[
        ['恋爱次数', resume.relationshipCount],
        ['分手核心原因', resume.breakupReason],
        ['恋爱短板', resume.loveShortcoming],
        ['从前感情里学到的东西', resume.loveInsight],
        ['自己在感情里的成长', resume.loveGrowth],
      ]} />

      <Section icon="🤝" title="恋爱相处模式" rows={[
        ['日常陪伴', resume.dailyCompany],
        ['吵架模式', resume.fightMode],
        ['表达爱意方式', resume.loveExpression],
        ['与异性边界', resume.oppositeBoundary],
      ]} />

      <Section icon="🌱" title="个人生活与规划" rows={[
        ['爱好与日常', resume.hobbies],
        ['日常状态', resume.dailyStatus],
        ['生活习惯', resume.lifeHabits],
        ['短期规划', resume.shortTermPlan],
        ['长期婚恋规划', resume.marriagePlan],
      ]} />

      <Section icon="💘" title="理想另一半" rows={[
        ['硬性条件', resume.hardConditions],
        ['软性期待', resume.softExpectations],
      ]} />

      <Section icon="⭐" title="加分项" rows={[
        ['我能为恋爱带来什么', resume.bonusPoints],
      ]} />

      <Section icon="💌" title="走心宣言" rows={[
        ['对爱情的期待', resume.loveExpectation],
        ['对新恋情的态度及承诺', resume.loveAttitude],
      ]} />

      {resume.photoAlbum && resume.photoAlbum.length > 0 && (
        <section className="resume-card">
          <h3 className="resume-card-title">📷 生活相册</h3>
          <div className="resume-album">
            {resume.photoAlbum.map((url, i) => (
              <img key={i} src={url} alt="" className="resume-album-img" />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

function Section({
  icon, title, rows, children,
}: {
  icon: string; title: string; rows: [string, any][]; children?: React.ReactNode;
}) {
  const list = rows.filter(([, v]) => v !== undefined && v !== null && String(v) !== '');
  if (list.length === 0 && !children) return null;
  return (
    <section className="resume-card">
      <h3 className="resume-card-title">{icon} {title}</h3>
      {children}
      {list.map(([label, value]) => (
        <div key={label} className="resume-row">
          <span className="resume-label">{label}</span>
          <span className="resume-value">{String(value)}</span>
        </div>
      ))}
    </section>
  );
}
