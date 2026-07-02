import { useEffect, useState } from 'react';
import { Table, Button, Input, Space, Tag, Popconfirm, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface UserRecord {
  id: number; nickname: string; phone: string; school: string;
  status: number; realNameVerified: number; createdAt: string;
}

export default function Users() {
  const [data, setData] = useState<UserRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');

  const fetchData = (p = 1, kw?: string) => {
    setLoading(true);
    request.get('/admin/users', { params: { page: p, size: 10, keyword: kw || keyword } })
      .then((res) => {
        setData(res.data.data.records);
        setTotal(res.data.data.total);
        setPage(p);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1); }, []);

  const toggleStatus = async (record: UserRecord) => {
    const newStatus = record.status === 1 ? 0 : 1;
    try {
      await request.put(`/admin/users/${record.id}/status`, { status: newStatus });
      message.success('已更新');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<UserRecord> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '昵称', dataIndex: 'nickname' },
    { title: '手机号', dataIndex: 'phone' },
    { title: '学校', dataIndex: 'school' },
    {
      title: '认证状态', dataIndex: 'realNameVerified', render: (v: number) => {
        const map: Record<number, { label: string; color: string }> = {
          0: { label: '未认证', color: 'default' },
          1: { label: '审核中', color: 'processing' },
          2: { label: '已认证', color: 'success' },
          3: { label: '已拒绝', color: 'error' },
        };
        return <Tag color={map[v]?.color}>{map[v]?.label}</Tag>;
      },
    },
    {
      title: '状态', dataIndex: 'status', render: (v: number) => (
        <Tag color={v === 1 ? 'success' : 'error'}>{v === 1 ? '正常' : '禁用'}</Tag>
      ),
    },
    { title: '注册时间', dataIndex: 'createdAt', render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', render: (_, record) => (
        <Space>
          <Popconfirm
            title={`确定${record.status === 1 ? '禁用' : '解禁'}该用户？`}
            onConfirm={() => toggleStatus(record)}
          >
            <a style={{ color: record.status === 1 ? 'red' : 'green' }}>
              {record.status === 1 ? '禁用' : '解禁'}
            </a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>用户管理</h2>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="搜索用户/手机号"
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
