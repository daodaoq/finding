import { useEffect, useState } from 'react';
import { Table, Space, Tag, Image, Modal, Input, message, theme } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface ModerationRecord {
  id: number;
  userId?: number;
  userNickname: string;
  imageUrl?: string;
  scene?: string;
  riskLevel?: string;
  ocrText?: string;
  createdAt: string;
}

const SCENE_LABEL: Record<string, string> = {
  avatar: '头像', profile_background: '背景图', post: '动态图', chat: '聊天图', album: '相册',
};

export default function ImageModeration() {
  const { token } = theme.useToken();
  const [data, setData] = useState<ModerationRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [rejectTarget, setRejectTarget] = useState<ModerationRecord | null>(null);
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchData = (p = 1) => {
    setLoading(true);
    request.get('/admin/image-moderation', { params: { page: p, size: 10 } })
      .then((res) => { setData(res.data.data.records); setTotal(res.data.data.total); setPage(p); })
      .catch(() => message.error('获取图片审核队列失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1); }, []);

  const handle = async (id: number, pass: boolean) => {
    setSubmitting(true);
    try {
      await request.put(`/admin/image-moderation/${id}/handle`, { pass, note: note.trim() || undefined });
      message.success(pass ? '已放行' : '已删除图片');
      setRejectTarget(null);
      setNote('');
      fetchData(page);
    } catch { message.error('操作失败'); }
    finally { setSubmitting(false); }
  };

  const columns: ColumnsType<ModerationRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '上传者', dataIndex: 'userNickname', width: 120 },
    {
      title: '图片', dataIndex: 'imageUrl', width: 90,
      render: (v: string) => v ? <Image src={v} width={48} height={48} style={{ objectFit: 'cover', borderRadius: 4 }} /> : <span style={{ color: token.colorTextTertiary }}>—</span>,
    },
    { title: '场景', dataIndex: 'scene', width: 90, render: (v: string) => SCENE_LABEL[v] || v || '—' },
    {
      title: '风险等级', dataIndex: 'riskLevel', width: 100,
      render: (v: string) => {
        const color = v === 'block' || v === 'high' ? 'red' : v === 'review' || v === 'medium' ? 'orange' : 'green';
        return <Tag color={color}>{v || '—'}</Tag>;
      },
    },
    { title: 'OCR 文字', dataIndex: 'ocrText', ellipsis: true },
    { title: '提交时间', dataIndex: 'createdAt', width: 160, render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', width: 140,
      render: (_, record) => (
        <Space>
          <a style={{ color: token.colorSuccess }} onClick={() => handle(record.id, true)}>放行</a>
          <a style={{ color: token.colorError }} onClick={() => setRejectTarget(record)}>删除</a>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>图片审核</h2>
      <p style={{ color: token.colorTextSecondary, marginBottom: 12 }}>机器送审(中等风险)的图片进入此队列,人工复核后放行或删除。</p>
      <Table
        columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{ current: page, total, pageSize: 10, onChange: (p) => fetchData(p), showTotal: (t) => `共 ${t} 条` }}
      />

      <Modal
        title="删除违规图片"
        open={rejectTarget != null}
        onOk={() => rejectTarget && handle(rejectTarget.id, false)}
        onCancel={() => setRejectTarget(null)}
        okText="确认删除"
        okButtonProps={{ danger: true }}
        confirmLoading={submitting}
        width={440}
      >
        {rejectTarget?.ocrText && <p style={{ color: token.colorTextSecondary }}>识别文字：{rejectTarget.ocrText}</p>}
        <Input.TextArea rows={3} placeholder="删除原因（可选）" value={note} onChange={(e) => setNote(e.target.value)} />
      </Modal>
    </div>
  );
}
