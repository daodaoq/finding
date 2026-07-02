import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Popconfirm, Modal, Input, message, Switch } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface BannerRecord {
  id: number; title: string; imageUrl: string; linkUrl: string;
  sortOrder: number; isActive: number;
}

export default function Banners() {
  const [data, setData] = useState<BannerRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<BannerRecord | null>(null);
  const [form, setForm] = useState({ title: '', imageUrl: '', linkUrl: '', sortOrder: 0, isActive: 1 });

  const fetchData = () => {
    setLoading(true);
    request.get('/admin/banners')
      .then((res) => setData(res.data.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(); }, []);

  const openCreate = () => {
    setEditing(null);
    setForm({ title: '', imageUrl: '', linkUrl: '', sortOrder: data.length + 1, isActive: 1 });
    setModalOpen(true);
  };

  const openEdit = (record: BannerRecord) => {
    setEditing(record);
    setForm({ title: record.title, imageUrl: record.imageUrl, linkUrl: record.linkUrl || '',
      sortOrder: record.sortOrder, isActive: record.isActive });
    setModalOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editing) {
        await request.put(`/admin/banners/${editing.id}`, form);
        message.success('已更新');
      } else {
        await request.post('/admin/banners', form);
        message.success('已创建');
      }
      setModalOpen(false);
      fetchData();
    } catch { message.error('操作失败'); }
  };

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/admin/banners/${id}`);
      message.success('已删除');
      fetchData();
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<BannerRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => i + 1 },
    { title: '标题', dataIndex: 'title' },
    { title: '图片', dataIndex: 'imageUrl', render: (url: string) => (
      <img src={url} alt="" style={{ width: 120, height: 50, objectFit: 'cover', borderRadius: 4 }} />
    )},
    { title: '排序', dataIndex: 'sortOrder', width: 60 },
    { title: '状态', dataIndex: 'isActive', render: (v: number) => (
      <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag>
    )},
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
      <h2 style={{ marginBottom: 16 }}>首页轮播管理</h2>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={openCreate}>+ 新增轮播</Button>
      </Space>
      <Table columns={columns} dataSource={data} rowKey="id" loading={loading} />

      <Modal
        title={editing ? '编辑轮播' : '新增轮播'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Input placeholder="标题" value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })} />
          <Input placeholder="图片URL" value={form.imageUrl}
            onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
          <Input placeholder="跳转链接（可选）" value={form.linkUrl}
            onChange={(e) => setForm({ ...form, linkUrl: e.target.value })} />
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span>排序:</span>
            <Input type="number" value={form.sortOrder} style={{ width: 80 }}
              onChange={(e) => setForm({ ...form, sortOrder: Number(e.target.value) })} />
            <span style={{ marginLeft: 16 }}>启用:</span>
            <Switch checked={form.isActive === 1}
              onChange={(v) => setForm({ ...form, isActive: v ? 1 : 0 })} />
          </div>
        </div>
      </Modal>
    </div>
  );
}
