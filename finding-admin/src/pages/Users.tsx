import { Table, Button, Input, Space, Tag, Popconfirm, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';

interface UserRecord {
  id: number; nickname: string; phone: string; school: string;
  status: number; realNameVerified: number; createdAt: string;
}

const mockData: UserRecord[] = [
  { id: 1, nickname: '张同学', phone: '138****0001', school: '山东理工大学', status: 1, realNameVerified: 2, createdAt: '2026-06-01' },
  { id: 2, nickname: '李同学', phone: '138****0002', school: '山东理工大学', status: 1, realNameVerified: 1, createdAt: '2026-06-10' },
];

export default function Users() {
  const columns: ColumnsType<UserRecord> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '昵称', dataIndex: 'nickname' },
    { title: '手机号', dataIndex: 'phone' },
    { title: '学校', dataIndex: 'school' },
    { title: '认证状态', dataIndex: 'realNameVerified', render: (v: number) => {
      const map: Record<number, { label: string; color: string }> = {
        0: { label: '未认证', color: 'default' },
        1: { label: '审核中', color: 'processing' },
        2: { label: '已认证', color: 'success' },
        3: { label: '已拒绝', color: 'error' },
      };
      return <Tag color={map[v]?.color}>{map[v]?.label}</Tag>;
    }},
    { title: '状态', dataIndex: 'status', render: (v: number) => (
      <Tag color={v === 1 ? 'success' : 'error'}>{v === 1 ? '正常' : '禁用'}</Tag>
    )},
    { title: '注册时间', dataIndex: 'createdAt' },
    { title: '操作', render: (_, record) => (
      <Space>
        <a>查看</a>
        <Popconfirm title="确定禁用该用户？" onConfirm={() => message.info('已操作')}>
          <a style={{ color: 'red' }}>{record.status === 1 ? '禁用' : '解禁'}</a>
        </Popconfirm>
      </Space>
    )},
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>用户管理</h2>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search placeholder="搜索用户/手机号" style={{ width: 300 }} />
        <Button type="primary">查询</Button>
      </Space>
      <Table columns={columns} dataSource={mockData} rowKey="id" />
    </div>
  );
}
