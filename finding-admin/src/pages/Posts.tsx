import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Input, Popconfirm, Select, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface PostRecord {
  id: number; content: string; userId: number; userNickname: string;
  likeCount: number; commentCount: number; status: number; createdAt: string;
}

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '正常', color: 'success' },
  0: { label: '已删除', color: 'error' },
  2: { label: '已隐藏', color: 'warning' },
};

export default function Posts() {
  const [data, setData] = useState<PostRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');

  const fetchData = (p = 1, kw?: string) => {
    setLoading(true);
    request.get('/admin/posts', { params: { page: p, size: 10, keyword: kw || keyword } })
      .then((res) => {
        setData(res.data.data.records);
        setTotal(res.data.data.total);
        setPage(p);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1); }, []);

  const updateStatus = async (id: number, status: number) => {
    try {
      await request.put(`/admin/posts/${id}/status`, { status });
      message.success('已更新');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/admin/posts/${id}`);
      message.success('已删除');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<PostRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '内容', dataIndex: 'content', ellipsis: true },
    { title: '发布者', dataIndex: 'userNickname', width: 100 },
    { title: '点赞', dataIndex: 'likeCount', width: 60 },
    { title: '评论', dataIndex: 'commentCount', width: 60 },
    {
      title: '状态', dataIndex: 'status', render: (v: number) => (
        <Tag color={STATUS_MAP[v]?.color}>{STATUS_MAP[v]?.label}</Tag>
      ),
    },
    { title: '发布时间', dataIndex: 'createdAt', render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', render: (_, record) => (
        <Space>
          {record.status !== 2 && (
            <Popconfirm title="确定隐藏该动态？" onConfirm={() => updateStatus(record.id, 2)}>
              <a>隐藏</a>
            </Popconfirm>
          )}
          {record.status === 2 && (
            <Popconfirm title="确定恢复该动态？" onConfirm={() => updateStatus(record.id, 1)}>
              <a style={{ color: 'green' }}>恢复</a>
            </Popconfirm>
          )}
          <Popconfirm title="确定删除该动态？" onConfirm={() => handleDelete(record.id)}>
            <a style={{ color: 'red' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>动态管理</h2>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="搜索内容或ID"
          style={{ width: 300 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={(v) => fetchData(1, v)}
        />
      </Space>
      <Table
        columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{
          current: page, total, pageSize: 10,
          onChange: (p) => fetchData(p),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />
    </div>
  );
}
