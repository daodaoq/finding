import { Table, Button, Space, Tag, Popconfirm, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';

interface VerifyRecord {
  id: number; realName: string; studentId: string; school: string;
  userId: number; status: number; createdAt: string;
}

const mockData: VerifyRecord[] = [
  { id: 1, realName: '张同学', studentId: '2024001', school: '山东理工大学', userId: 1, status: 0, createdAt: '2026-06-28' },
  { id: 2, realName: '李同学', studentId: '2024002', school: '山东理工大学', userId: 2, status: 0, createdAt: '2026-06-29' },
];

export default function Verification() {
  const columns: ColumnsType<VerifyRecord> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '真实姓名', dataIndex: 'realName' },
    { title: '学号', dataIndex: 'studentId' },
    { title: '学校', dataIndex: 'school' },
    { title: '状态', dataIndex: 'status', render: (v: number) => {
      const map: Record<number, { label: string; color: string }> = {
        0: { label: '待审核', color: 'processing' },
        1: { label: '已通过', color: 'success' },
        2: { label: '已拒绝', color: 'error' },
      };
      return <Tag color={map[v]?.color}>{map[v]?.label}</Tag>;
    }},
    { title: '提交时间', dataIndex: 'createdAt' },
    { title: '操作', render: (_, record) => (
      <Space>
        <Popconfirm title="确认通过？" onConfirm={() => message.success('已通过')}>
          <Button type="primary" size="small">通过</Button>
        </Popconfirm>
        <Popconfirm title="确认拒绝？" onConfirm={() => message.info('已拒绝')}>
          <Button danger size="small">拒绝</Button>
        </Popconfirm>
        <a>查看材料</a>
      </Space>
    )},
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>实名认证审核</h2>
      <Table columns={columns} dataSource={mockData} rowKey="id" />
    </div>
  );
}
