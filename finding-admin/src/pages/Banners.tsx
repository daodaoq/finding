import { Table, Button, Space, Tag, Switch, Popconfirm, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';

interface BannerRecord {
  id: number; title: string; imageUrl: string; linkUrl: string;
  sortOrder: number; isActive: number;
}

const mockData: BannerRecord[] = [
  { id: 1, title: '新生交友季', imageUrl: '/uploads/banner1.jpg', linkUrl: '/activity/1', sortOrder: 1, isActive: 1 },
  { id: 2, title: '搭子匹配', imageUrl: '/uploads/banner2.jpg', linkUrl: '/mate', sortOrder: 2, isActive: 1 },
];

export default function Banners() {
  const columns: ColumnsType<BannerRecord> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '标题', dataIndex: 'title' },
    { title: '图片', dataIndex: 'imageUrl', render: (url: string) => (
      <img src={url} alt="" style={{ width: 120, height: 50, objectFit: 'cover', borderRadius: 4 }} />
    )},
    { title: '排序', dataIndex: 'sortOrder', width: 60 },
    { title: '状态', dataIndex: 'isActive', render: (v: number) => (
      <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag>
    )},
    { title: '操作', render: () => (
      <Space>
        <a>编辑</a>
        <Popconfirm title="确定删除？" onConfirm={() => message.info('已删除')}>
          <a style={{ color: 'red' }}>删除</a>
        </Popconfirm>
      </Space>
    )},
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>首页轮播管理</h2>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary">+ 新增轮播</Button>
      </Space>
      <Table columns={columns} dataSource={mockData} rowKey="id" />
    </div>
  );
}
