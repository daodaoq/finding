import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Popconfirm, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface FeedbackRecord {
  id: number; userId: number; nickname: string; type: string;
  content: string; contact?: string; status: number; createdAt: string;
}

const TYPE_LABEL: Record<string, string> = {
  bug: '问题反馈', feature: '功能建议', suggestion: '优化建议', other: '其他',
};

export default function Feedback() {
  const [data, setData] = useState<FeedbackRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState<number | undefined>(undefined);

  const fetchData = (p = 1, st?: number) => {
    setLoading(true);
    request.get('/admin/feedbacks', { params: { page: p, size: 10, status: st } })
      .then((res) => { setData(res.data.data.records); setTotal(res.data.data.total); setPage(p); })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1, status); }, [status]);

  const toggleStatus = async (record: FeedbackRecord) => {
    const newStatus = record.status === 1 ? 0 : 1;
    try {
      await request.put(`/admin/feedbacks/${record.id}/status`, { status: newStatus });
      message.success(newStatus === 1 ? '已处理' : '已重新打开');
      fetchData(page, status);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<FeedbackRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '用户', render: (_, r) => <span>{r.nickname}（{r.userId}）</span> },
    { title: '类型', dataIndex: 'type', width: 90, render: (v: string) => <Tag>{TYPE_LABEL[v] || v}</Tag> },
    { title: '内容', dataIndex: 'content', ellipsis: true },
    { title: '联系方式', dataIndex: 'contact', width: 130, render: (v?: string) => v || '-' },
    { title: '状态', dataIndex: 'status', width: 80, render: (v: number) => <Tag color={v === 1 ? 'success' : 'warning'}>{v === 1 ? '已处理' : '待处理'}</Tag> },
    { title: '提交时间', dataIndex: 'createdAt', render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', render: (_, r) => (
        <Popconfirm title={r.status === 1 ? '重新打开该工单？' : '标记为已处理？'} onConfirm={() => toggleStatus(r)}>
          <a style={{ color: r.status === 1 ? '#999' : '#52c41a' }}>
            {r.status === 1 ? '重新打开' : '标记已处理'}
          </a>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>用户反馈</h2>
      <Space style={{ marginBottom: 16 }}>
        {[{ v: undefined, l: '全部' }, { v: 0, l: '待处理' }, { v: 1, l: '已处理' }].map((t) => (
          <Button
            key={String(t.v)}
            type={status === t.v ? 'primary' : 'default'}
            size="small"
            onClick={() => setStatus(t.v)}
          >
            {t.l}
          </Button>
        ))}
      </Space>
      <Table
        columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{
          current: page, total, pageSize: 10,
          onChange: (p) => fetchData(p, status),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />
    </div>
  );
}
