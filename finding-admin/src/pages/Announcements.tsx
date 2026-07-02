import { useEffect, useState } from 'react';
import { Table, Button, Space, Popconfirm, message, Modal, Input } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface AnnouncementRecord {
  id: number; title: string; content: string; createdBy: number; createdAt: string;
}

export default function Announcements() {
  const [data, setData] = useState<AnnouncementRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AnnouncementRecord | null>(null);
  const [form, setForm] = useState({ title: '', content: '' });

  const fetchData = (p = 1) => {
    setLoading(true);
    request.get('/admin/announcements', { params: { page: p, size: 10 } })
      .then((res) => {
        setData(res.data.data.records);
        setTotal(res.data.data.total);
        setPage(p);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1); }, []);

  const openCreate = () => {
    setEditing(null);
    setForm({ title: '', content: '' });
    setModalOpen(true);
  };

  const openEdit = (record: AnnouncementRecord) => {
    setEditing(record);
    setForm({ title: record.title, content: record.content });
    setModalOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editing) {
        await request.put(`/admin/announcements/${editing.id}`, form);
        message.success('已更新');
      } else {
        await request.post('/admin/announcements', form);
        message.success('已发布');
      }
      setModalOpen(false);
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/admin/announcements/${id}`);
      message.success('已删除');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<AnnouncementRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '标题', dataIndex: 'title' },
    { title: '内容', dataIndex: 'content', ellipsis: true },
    { title: '发布时间', dataIndex: 'createdAt', render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', render: (_, record) => (
        <Space>
          <a onClick={() => openEdit(record)}>编辑</a>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <a style={{ color: 'red' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>系统公告</h2>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={openCreate}>+ 新建公告</Button>
      </Space>
      <Table
        columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{
          current: page, total, pageSize: 10,
          onChange: (p) => fetchData(p),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />

      <Modal
        title={editing ? '编辑公告' : '新建公告'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        okText={editing ? '保存' : '发布'}
      >
        <Input
          placeholder="公告标题"
          style={{ marginBottom: 12 }}
          value={form.title}
          onChange={(e) => setForm({ ...form, title: e.target.value })}
        />
        <Input.TextArea
          rows={4}
          placeholder="公告内容"
          value={form.content}
          onChange={(e) => setForm({ ...form, content: e.target.value })}
        />
      </Modal>
    </div>
  );
}
