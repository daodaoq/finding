import { Table, Button, Space, Tag, Popconfirm, message, Modal, Input } from 'antd';
import { useState } from 'react';
import type { ColumnsType } from 'antd/es/table';

interface AnnouncementRecord {
  id: number; title: string; content: string; createdBy: number; createdAt: string;
}

const mockData: AnnouncementRecord[] = [
  { id: 1, title: '关于平台实名认证的通知', content: '请所有用户在7月15日前完成学生实名认证...', createdBy: 1, createdAt: '2026-07-01' },
];

export default function Announcements() {
  const [modalOpen, setModalOpen] = useState(false);

  const columns: ColumnsType<AnnouncementRecord> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '标题', dataIndex: 'title' },
    { title: '内容', dataIndex: 'content', ellipsis: true },
    { title: '发布者', dataIndex: 'createdBy', width: 80 },
    { title: '发布时间', dataIndex: 'createdAt' },
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
      <h2 style={{ marginBottom: 16 }}>系统公告</h2>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => setModalOpen(true)}>+ 新建公告</Button>
      </Space>
      <Table columns={columns} dataSource={mockData} rowKey="id" />

      <Modal title="新建公告" open={modalOpen} onCancel={() => setModalOpen(false)} onOk={() => { message.success('已发布'); setModalOpen(false); }}>
        <Input placeholder="公告标题" style={{ marginBottom: 12 }} />
        <Input.TextArea rows={4} placeholder="公告内容" />
      </Modal>
    </div>
  );
}
