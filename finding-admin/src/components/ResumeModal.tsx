import { useEffect, useState } from 'react';
import { Modal, Descriptions, Empty, Image, Spin, Tag } from 'antd';
import request from '../api/request';
import type { ResumeFieldValue } from '../types/admin';

interface ResumeData {
  gender?: number;
  age?: number;
  birthday?: string;
  constellation?: string;
  heightCm?: number;
  weightKg?: number;
  campus?: string;
  majorGrade?: string;
  hometown?: string;
  career?: string;
  dailyRoutine?: string;
  relationshipStatus?: string;
  coreBottomLine?: string;
  mbti?: string;
  personalityTraits?: string;
  inLoveLook?: string;
  flaws?: string;
  worldview?: string;
  personalTags?: string;
  relationshipCount?: string;
  breakupReason?: string;
  loveShortcoming?: string;
  loveInsight?: string;
  loveGrowth?: string;
  dailyCompany?: string;
  fightMode?: string;
  loveExpression?: string;
  oppositeBoundary?: string;
  dailyStatus?: string;
  lifeHabits?: string;
  shortTermPlan?: string;
  longTermPlan?: string;
  hobbies?: string;
  marriagePlan?: string;
  hardConditions?: string;
  softExpectations?: string;
  bonusPoints?: string;
  loveExpectation?: string;
  loveAttitude?: string;
  photoAlbum?: string[];
}

const BLOCKS: { title: string; fields: { key: keyof ResumeData; label: string; render?: (v: ResumeFieldValue) => string }[] }[] = [
  {
    title: '基础信息',
    fields: [
      { key: 'gender', label: '性别', render: (v) => (v === 1 ? '男' : v === 2 ? '女' : '') },
      { key: 'age', label: '年龄' },
      { key: 'birthday', label: '生日' },
      { key: 'constellation', label: '星座' },
      { key: 'heightCm', label: '身高(cm)' },
      { key: 'weightKg', label: '体重(kg)' },
      { key: 'campus', label: '校区' },
      { key: 'majorGrade', label: '专业年级' },
      { key: 'hometown', label: '家乡' },
      { key: 'career', label: '职业' },
      { key: 'dailyRoutine', label: '日常作息' },
      { key: 'relationshipStatus', label: '恋爱状态' },
      { key: 'coreBottomLine', label: '择偶核心底线' },
    ],
  },
  {
    title: '自我画像',
    fields: [
      { key: 'mbti', label: 'MBTI' },
      { key: 'personalityTraits', label: '性格特质' },
      { key: 'inLoveLook', label: '恋爱中的样子' },
      { key: 'flaws', label: '小缺点' },
      { key: 'worldview', label: '个人三观' },
      { key: 'personalTags', label: '个人标签' },
    ],
  },
  {
    title: '恋爱复盘',
    fields: [
      { key: 'relationshipCount', label: '恋爱次数' },
      { key: 'breakupReason', label: '分手客观原因' },
      { key: 'loveShortcoming', label: '恋爱短板' },
      { key: 'loveInsight', label: '恋爱感悟' },
      { key: 'loveGrowth', label: '感情里的成长' },
    ],
  },
  {
    title: '恋爱相处模式',
    fields: [
      { key: 'dailyCompany', label: '日常陪伴' },
      { key: 'fightMode', label: '吵架模式' },
      { key: 'loveExpression', label: '表达爱意方式' },
      { key: 'oppositeBoundary', label: '与异性边界' },
    ],
  },
  {
    title: '个人生活与规划',
    fields: [
      { key: 'dailyStatus', label: '日常状态' },
      { key: 'lifeHabits', label: '生活习惯' },
      { key: 'shortTermPlan', label: '短期规划' },
      { key: 'longTermPlan', label: '长期规划' },
      { key: 'hobbies', label: '爱好与日常' },
      { key: 'marriagePlan', label: '长期婚恋规划' },
    ],
  },
  {
    title: '理想的另一半',
    fields: [
      { key: 'hardConditions', label: '硬性条件' },
      { key: 'softExpectations', label: '软性期待' },
    ],
  },
  {
    title: '加分项',
    fields: [{ key: 'bonusPoints', label: '我能为恋爱带来什么' }],
  },
  {
    title: '走心宣言',
    fields: [
      { key: 'loveExpectation', label: '对爱情的期待' },
      { key: 'loveAttitude', label: '对新恋情的态度' },
    ],
  },
];

export default function ResumeModal({ userId, open, onClose }: { userId: number | null; open: boolean; onClose: () => void }) {
  const [resume, setResume] = useState<ResumeData | null>(null);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    if (!open || !userId) return;
    setLoading(true);
    setResume(null);
    setLoaded(false);
    request.get(`/admin/users/${userId}/resume`)
      .then((res) => {
        setResume(res.data.data);
        setLoaded(true);
      })
      .catch(() => { setLoaded(true); })
      .finally(() => setLoading(false));
  }, [open, userId]);

  const hasValue = (v: ResumeFieldValue) => v !== null && v !== undefined && v !== '';

  const valueOf = (field: { key: keyof ResumeData; label: string; render?: (v: ResumeFieldValue) => string }) => {
    if (!resume) return '';
    const v = resume[field.key];
    if (!hasValue(v)) return '';
    return field.render ? field.render(v) : String(v);
  };

  return (
    <Modal
      title="情感简历"
      open={open}
      onCancel={onClose}
      footer={null}
      width={640}
    >
      <Spin spinning={loading}>
        {loaded && !resume && <Empty description="该用户未填写情感简历" />}
        {loaded && resume && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            {BLOCKS.map((block) => {
              const hasAny = block.fields.some((f) => hasValue(resume[f.key]));
              if (!hasAny) return null;
              return (
                <div key={block.title}>
                  <div style={{ fontWeight: 600, marginBottom: 8, color: '#ff6b81' }}>{block.title}</div>
                  <Descriptions
                    size="small"
                    column={2}
                    items={block.fields
                      .filter((f) => valueOf(f) !== '')
                      .map((f) => ({
                        key: f.key as string,
                        label: f.label,
                        children: f.key === 'personalTags' ? (
                          <span>
                            {String(resume[f.key]).split(',').filter(Boolean).map((t) => (
                              <Tag key={t} style={{ marginBottom: 4 }}>{t}</Tag>
                            ))}
                          </span>
                        ) : valueOf(f),
                      }))}
                  />
                </div>
              );
            })}
            {(resume.photoAlbum ?? []).length > 0 && (
              <div>
                <div style={{ fontWeight: 600, marginBottom: 8, color: '#ff6b81' }}>生活相册</div>
                <Image.PreviewGroup>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                    {resume.photoAlbum!.map((url, i) => (
                      <Image key={i} src={url} width={96} height={96} style={{ objectFit: 'cover', borderRadius: 6 }} />
                    ))}
                  </div>
                </Image.PreviewGroup>
              </div>
            )}
          </div>
        )}
      </Spin>
    </Modal>
  );
}
