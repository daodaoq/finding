import { Table, Button, Space, Tag, Input, Popconfirm, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';

interface PostRecord {
  id: number; content: string; userId: number; likeCount: number;
  commentCount: number; status: number; createdAt: string;
}

const mockData: PostRecord[] = [
  { id: 1, content: '今天天气真好，有人一起跑步吗？🏃', userId: 1, likeCount: 23, commentCount: 5, status: 1, createdAt: '2026-07-01' },
  { id: 2, content: '求图书馆一起学习的搭子 📚', userId: 2, likeCount: 15, commentCount: 8, status: 1, createdAt: '2026-07-01' },
];

export default function Posts() {
  const columns: ColumnsType<PostRecord> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '内容', dataIndex: 'content', ellipsis: true },
    { title: '发布者ID', dataIndex: 'userId', width: 80 },
    { title: '点赞', dataIndex: 'likeCount', width: 60 },
    { title: '评论', dataIndex: 'commentCount', width: 60 },
    { title: '状态', dataIndex: 'status', render: (v: number) => (
      <Tag color={v === 1 ? 'success' : v === 0 ? 'error' : 'warning'}>
        {v === 1 ? '正常' : v === 0 ? '已删除' : '已隐藏'}
      </Tag>
    )},
    { title: '发布时间', dataIndex: 'createdAt' },
    { title: '操作', render: (_, record) => (
      <Space>
        <a>查看</a>
        <Popconfirm title="确定隐藏该动态？" onConfirm={() => message.info('已操作')}>
          <a>隐藏</a>
        </Popconfirm>
        <Popconfirm title="确定删除该动态？" onConfirm={() => message.info('已删除')}>
          <a style={{ color: 'red' }}>删除</a>
        </Popconfirm>
      </Space>
    )},
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>动态管理</h2>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search placeholder="搜索内容或ID" style={{ width: 300 }} />
        <Button type="primary">查询</Button>
      </Space>
      <Table columns={columns} dataSource={mockData} rowKey="id" />
    </div>
  );
}
