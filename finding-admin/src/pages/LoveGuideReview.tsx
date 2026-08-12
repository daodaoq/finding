import { useEffect, useState } from 'react';
import { Button, Input, Modal, Space, Table, Tag, message, theme } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface Guide { id: number; title: string; subtitle: string; content: string; category: string; userId: number; reviewStatus: number; createdAt: string; }

export default function LoveGuideReview() {
  const { token } = theme.useToken(); const [data, setData] = useState<Guide[]>([]); const [page, setPage] = useState(1); const [total, setTotal] = useState(0); const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<Guide | null>(null); const [reject, setReject] = useState<Guide | null>(null); const [reason, setReason] = useState('');
  const load = (p = 1) => { setLoading(true); request.get('/admin/love-guides/review', { params: { page: p, size: 10 } }).then(r => { setData(r.data.data.records); setTotal(r.data.data.total); setPage(p); }).catch(() => message.error('获取待审核投稿失败')).finally(() => setLoading(false)); };
  useEffect(() => { load(); }, []);
  const review = async (guide: Guide, pass: boolean) => { try { await request.put(`/admin/love-guides/${guide.id}/review`, { pass, reason: pass ? undefined : reason.trim() || undefined }); message.success(pass ? '已通过并公开展示' : '已拒绝并通知投稿用户'); setReject(null); setReason(''); load(page); } catch { message.error('操作失败'); } };
  const columns: ColumnsType<Guide> = [
    { title: '标题', dataIndex: 'title', width: 220, ellipsis: true }, { title: '分类', dataIndex: 'category', width: 110, render: v => <Tag color="pink">{v}</Tag> },
    { title: '副标题', dataIndex: 'subtitle', ellipsis: true }, { title: '投稿用户', dataIndex: 'userId', width: 100 }, { title: '提交时间', dataIndex: 'createdAt', width: 165, render: v => v?.replace('T', ' ') },
    { title: '状态', dataIndex: 'reviewStatus', width: 90, render: v => v === 0 ? <Tag color="processing">待审核</Tag> : v === 1 ? <Tag color="success">已通过</Tag> : <Tag color="error">已拒绝</Tag> },
    { title: '操作', width: 175, render: (_, row) => <Space><a onClick={() => setDetail(row)}>查看</a>{row.reviewStatus === 0 && <><a style={{ color: token.colorSuccess }} onClick={() => review(row, true)}>通过</a><a style={{ color: token.colorError }} onClick={() => setReject(row)}>拒绝</a></>}</Space> },
  ];
  return <div><h2 style={{ marginBottom: 16 }}>恋爱干货审核</h2><Table rowKey="id" columns={columns} dataSource={data} loading={loading} pagination={{ current: page, total, pageSize: 10, onChange: load, showTotal: t => `共 ${t} 条` }} />
    <Modal title="投稿内容" open={!!detail} footer={null} onCancel={() => setDetail(null)} width={640}>{detail && <><Tag color="pink">{detail.category}</Tag><h2 style={{ margin: '12px 0 4px' }}>{detail.title}</h2><p style={{ color: '#888', marginBottom: 18 }}>{detail.subtitle}</p><article style={{ whiteSpace: 'pre-wrap', lineHeight: 1.8 }}>{detail.content}</article></>}</Modal>
    <Modal title="拒绝投稿" open={!!reject} okText="确认拒绝" okButtonProps={{ danger: true }} onOk={() => reject && review(reject, false)} onCancel={() => setReject(null)}><p>拒绝原因会通知给投稿用户。</p><Input.TextArea rows={3} value={reason} onChange={e => setReason(e.target.value)} placeholder="拒绝原因（可选）" /></Modal>
  </div>;
}
