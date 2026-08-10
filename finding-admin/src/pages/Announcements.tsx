import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Radio, Popconfirm, message, Modal, Input } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import MDEditor from '@uiw/react-md-editor';
import '@uiw/react-md-editor/markdown-editor.css';
import '@uiw/react-markdown-preview/markdown.css';
import request from '../api/request';

interface AnnouncementRecord {
  id: number; title: string; content: string; type: number; status: number; createdBy: number; createdAt: string;
}

export default function Announcements() {
  const [data, setData] = useState<AnnouncementRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AnnouncementRecord | null>(null);
  const [form, setForm] = useState({ title: '', content: '', type: 1 });

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
    setForm({ title: '', content: '', type: 1 });
    setModalOpen(true);
  };

  const openEdit = (record: AnnouncementRecord) => {
    setEditing(record);
    setForm({ title: record.title, content: record.content, type: record.type || 1 });
    setModalOpen(true);
  };

  const handleSave = async () => {
    try {
      const body = { title: form.title, content: form.content, type: form.type };
      if (editing) {
        await request.put(`/admin/announcements/${editing.id}`, body);
        message.success('已更新');
      } else {
        await request.post('/admin/announcements', body);
        message.success('已发布');
      }
      setModalOpen(false);
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const toggleStatus = async (record: AnnouncementRecord) => {
    const newStatus = record.status === 1 ? 0 : 1;
    try {
      await request.put(`/admin/announcements/${record.id}/status`, { status: newStatus });
      message.success(newStatus === 1 ? '已上架' : '已下架');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/admin/announcements/${id}`);
      message.success('已撤回');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<AnnouncementRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '标题', dataIndex: 'title' },
    { title: '类型', dataIndex: 'type', width: 90, render: (v: number) => (
      <Tag color={v === 2 ? 'blue' : 'default'}>{v === 2 ? '永久展示' : '普通公告'}</Tag>
    )},
    { title: '内容', dataIndex: 'content', ellipsis: true },
    { title: '状态', dataIndex: 'status', width: 90, render: (v: number) => (
      <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '展示中' : '已下架'}</Tag>
    )},
    { title: '发布时间', dataIndex: 'createdAt', render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', render: (_, record) => (
        <Space>
          <a onClick={() => openEdit(record)}>编辑</a>
          <Popconfirm title={`确定${record.status === 1 ? '下架' : '上架'}该公告？`} onConfirm={() => toggleStatus(record)}>
            <a style={{ color: record.status === 1 ? 'orange' : 'green' }}>
              {record.status === 1 ? '下架' : '上架'}
            </a>
          </Popconfirm>
          <Popconfirm title="确定撤回？撤回后不可恢复" onConfirm={() => handleDelete(record.id)}>
            <a style={{ color: 'red' }}>撤回</a>
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
        width={760}
      >
        <Radio.Group
          value={form.type}
          onChange={(e) => setForm({ ...form, type: e.target.value })}
          style={{ marginBottom: 12 }}
        >
          <Radio value={1}>普通公告(发布后弹窗展示)</Radio>
          <Radio value={2}>永久展示(页面顶部悬浮横条)</Radio>
        </Radio.Group>
        <Input
          placeholder="公告标题"
          style={{ marginBottom: 12 }}
          value={form.title}
          onChange={(e) => setForm({ ...form, title: e.target.value })}
        />
        <div data-color-mode="light" style={{ marginBottom: 8 }}>
          <MDEditor
            height={320}
            value={form.content}
            onChange={(v) => setForm({ ...form, content: v || '' })}
            preview="live"
          />
        </div>
        <div style={{ fontSize: 12, color: '#bbb' }}>
          💡 标准 Markdown 编辑器：顶部工具栏可加粗/斜体/标题/列表/引用/代码/表格/图片等，支持实时预览。用户端会渲染为富文本。
        </div>
      </Modal>
    </div>
  );
}
