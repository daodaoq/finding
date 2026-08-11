import { useEffect, useState } from 'react';
import { Modal, Input, InputNumber, Select, Button, message } from 'antd';
import request from '../api/request';
import type { ResumeFieldValue, ResumeForm } from '../types/admin';

interface Field {
  key: string;
  label: string;
  type?: 'text' | 'textarea' | 'number' | 'date' | 'select';
  options?: { value: number | string; label: string }[];
}

const BLOCKS: { title: string; fields: Field[] }[] = [
  {
    title: '基础信息',
    fields: [
      { key: 'gender', label: '性别', type: 'select', options: [{ value: 1, label: '男' }, { value: 2, label: '女' }] },
      { key: 'age', label: '年龄', type: 'number' },
      { key: 'birthday', label: '生日', type: 'date' },
      { key: 'constellation', label: '星座' },
      { key: 'heightCm', label: '身高(cm)', type: 'number' },
      { key: 'weightKg', label: '体重(kg)', type: 'number' },
      { key: 'campus', label: '校区' },
      { key: 'majorGrade', label: '专业年级' },
      { key: 'hometown', label: '家乡' },
      { key: 'career', label: '职业' },
      { key: 'dailyRoutine', label: '日常作息' },
      { key: 'relationshipStatus', label: '恋爱状态' },
      { key: 'coreBottomLine', label: '择偶核心底线', type: 'textarea' },
    ],
  },
  {
    title: '自我画像',
    fields: [
      { key: 'mbti', label: 'MBTI' },
      { key: 'personalityTraits', label: '性格特质', type: 'textarea' },
      { key: 'inLoveLook', label: '恋爱中的样子', type: 'textarea' },
      { key: 'flaws', label: '小缺点', type: 'textarea' },
      { key: 'worldview', label: '个人三观', type: 'textarea' },
      { key: 'personalTags', label: '个人标签' },
    ],
  },
  {
    title: '恋爱复盘',
    fields: [
      { key: 'relationshipCount', label: '恋爱次数' },
      { key: 'breakupReason', label: '分手客观原因', type: 'textarea' },
      { key: 'loveShortcoming', label: '恋爱短板', type: 'textarea' },
      { key: 'loveInsight', label: '恋爱感悟', type: 'textarea' },
      { key: 'loveGrowth', label: '感情里的成长', type: 'textarea' },
    ],
  },
  {
    title: '恋爱相处模式',
    fields: [
      { key: 'dailyCompany', label: '日常陪伴', type: 'textarea' },
      { key: 'fightMode', label: '吵架模式', type: 'textarea' },
      { key: 'loveExpression', label: '表达爱意方式', type: 'textarea' },
      { key: 'oppositeBoundary', label: '与异性边界', type: 'textarea' },
    ],
  },
  {
    title: '个人生活与规划',
    fields: [
      { key: 'dailyStatus', label: '日常状态', type: 'textarea' },
      { key: 'lifeHabits', label: '生活习惯', type: 'textarea' },
      { key: 'shortTermPlan', label: '短期规划', type: 'textarea' },
      { key: 'longTermPlan', label: '长期规划', type: 'textarea' },
      { key: 'hobbies', label: '爱好与日常', type: 'textarea' },
      { key: 'marriagePlan', label: '长期婚恋规划', type: 'textarea' },
    ],
  },
  {
    title: '理想的另一半',
    fields: [
      { key: 'hardConditions', label: '硬性条件', type: 'textarea' },
      { key: 'softExpectations', label: '软性期待', type: 'textarea' },
    ],
  },
  {
    title: '加分项与走心宣言',
    fields: [
      { key: 'bonusPoints', label: '加分项', type: 'textarea' },
      { key: 'loveExpectation', label: '对爱情的期待', type: 'textarea' },
      { key: 'loveAttitude', label: '对新恋情的态度', type: 'textarea' },
    ],
  },
];

export default function ResumeEditModal({
  userId, open, onClose,
}: { userId: number | null; open: boolean; onClose: () => void }) {
  const [form, setForm] = useState<ResumeForm>({});
  const [photoText, setPhotoText] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open || !userId) return;
    setLoading(true);
    setForm({});
    setPhotoText('');
    request.get(`/admin/users/${userId}/resume`)
      .then((res) => {
        const r = res.data.data;
        if (r) {
          const f: ResumeForm = {};
          BLOCKS.forEach((b) => b.fields.forEach((fld) => {
            const v = r[fld.key] as ResumeFieldValue;
            if (v !== null && v !== undefined && v !== '') {
              f[fld.key] = v;
            }
          }));
          setForm(f);
          if (Array.isArray(r.photoAlbum)) setPhotoText(r.photoAlbum.join('\n'));
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [open, userId]);

  const setVal = (key: string, v: ResumeFieldValue) => setForm((prev) => ({ ...prev, [key]: v }));

  const save = async () => {
    if (!userId) return;
    setSaving(true);
    try {
      const photos = photoText.split('\n').map((s) => s.trim()).filter(Boolean);
      await request.put(`/admin/users/${userId}/resume`, { ...form, photoAlbum: photos });
      message.success('已保存');
      onClose();
    } catch { message.error('保存失败'); }
    finally { setSaving(false); }
  };

  const renderField = (f: Field) => {
    const value = form[f.key];
    if (f.type === 'select') {
      return (
        <Select
          style={{ width: '100%' }}
          allowClear
          placeholder={`${f.label}（可留空）`}
          value={value}
          onChange={(v) => setVal(f.key, v)}
          options={f.options}
        />
      );
    }
    if (f.type === 'number') {
      return (
        <InputNumber
          style={{ width: '100%' }}
          placeholder={`${f.label}（可留空）`}
          value={value as number}
          onChange={(v) => setVal(f.key, v ?? undefined)}
        />
      );
    }
    if (f.type === 'date') {
      return (
        <Input
          type="date"
          value={value || ''}
          onChange={(e) => setVal(f.key, e.target.value || undefined)}
        />
      );
    }
    if (f.type === 'textarea') {
      return (
        <Input.TextArea
          rows={2}
          placeholder={`${f.label}（可留空）`}
          value={value || ''}
          onChange={(e) => setVal(f.key, e.target.value || undefined)}
        />
      );
    }
    return (
      <Input
        placeholder={`${f.label}（可留空）`}
        value={value || ''}
        onChange={(e) => setVal(f.key, e.target.value || undefined)}
      />
    );
  };

  return (
    <Modal
      title="编辑情感简历"
      open={open}
      onOk={save}
      onCancel={onClose}
      okText="保存"
      confirmLoading={saving}
      width={680}
    >
      {loading ? (
        <div style={{ textAlign: 'center', color: '#999', padding: 30 }}>加载中...</div>
      ) : (
        <div style={{ maxHeight: '60vh', overflowY: 'auto', paddingRight: 8 }}>
          {BLOCKS.map((b) => (
            <div key={b.title} style={{ marginBottom: 18 }}>
              <div style={{ fontWeight: 600, marginBottom: 8, color: '#ff6b81' }}>{b.title}</div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px 14px' }}>
                {b.fields.map((f) => (
                  <div key={f.key} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                    <span style={{ fontSize: 12, color: '#888' }}>{f.label}</span>
                    {renderField(f)}
                  </div>
                ))}
              </div>
            </div>
          ))}
          <div style={{ marginBottom: 18 }}>
            <div style={{ fontWeight: 600, marginBottom: 8, color: '#ff6b81' }}>生活相册</div>
            <Input.TextArea
              rows={3}
              placeholder="每行一个图片 URL（可留空）"
              value={photoText}
              onChange={(e) => setPhotoText(e.target.value)}
            />
            <div style={{ marginTop: 4, fontSize: 12, color: '#bbb' }}>每行一个图片链接</div>
          </div>
          <Button type="link" onClick={() => setForm({})}>清空全部字段</Button>
        </div>
      )}
    </Modal>
  );
}
