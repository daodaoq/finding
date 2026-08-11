import { useEffect, useState } from 'react';
import { Table, Space, Modal, Input, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface ReviewItem {
  id: number;
  title: string;
  description?: string;
  userNickname: string;
  createdAt: string;
}

export default function MateReview() {
  const [data, setData] = useState<ReviewItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [rejectTarget, setRejectTarget] = useState<ReviewItem | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchData = (p = 1) => {
    setLoading(true);
    request.get('/admin/mates/review', { params: { page: p, size: 10 } })
      .then((res) => { setData(res.data.data.records); setTotal(res.data.data.total); setPage(p); })
      .catch(() => message.error('获取待审搭子失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1); }, []);

  const approve = async (id: number) => {
    try {
      await request.put(`/admin/mates/${id}/review`, { pass: true });
      message.success('已通过并发布');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const confirmReject = async () => {
    if (!rejectTarget) return;
    setSubmitting(true);
    try {
      await request.put(`/admin/mates/${rejectTarget.id}/review`, {
        pass: false,
        reason: rejectReason.trim() || undefined,
      });
      message.success('已拒绝并通知作者');
      setRejectTarget(null);
      setRejectReason('');
      fetchData(page);
    } catch { message.error('操作失败'); }
    finally { setSubmitting(false); }
  };

  const columns: ColumnsType<ReviewItem> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    { title: '作者', dataIndex: 'userNickname', width: 120 },
    { title: '提交时间', dataIndex: 'createdAt', width: 160, render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', width: 140,
      render: (_, record) => (
        <Space>
          <a style={{ color: '#52c41a' }} onClick={() => approve(record.id)}>通过</a>
          <a style={{ color: '#f5222d' }} onClick={() => setRejectTarget(record)}>拒绝</a>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>搭子审核队列</h2>
      <Table
        columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{ current: page, total, pageSize: 10, onChange: (p) => fetchData(p), showTotal: (t) => `共 ${t} 条` }}
      />
      <Modal
        title="拒绝该搭子"
        open={rejectTarget != null}
        onOk={confirmReject}
        onCancel={() => setRejectTarget(null)}
        okText="确认拒绝"
        okButtonProps={{ danger: true }}
        confirmLoading={submitting}
        width={460}
      >
        <p style={{ marginBottom: 8 }}>拒绝原因会通知给作者:</p>
        <Input.TextArea rows={3} placeholder="如：包含推广引流内容" value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} />
      </Modal>
    </div>
  );
}
