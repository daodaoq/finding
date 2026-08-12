import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Popconfirm, Modal, Input, message, Switch, Upload, theme } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadRequestOption } from 'rc-upload/lib/interface';
import request from '../api/request';

interface BannerRecord {
  id: number; title: string; imageUrl: string; linkUrl: string;
  sortOrder: number; isActive: number;
}

export default function Banners() {
  const { token } = theme.useToken();
  const [data, setData] = useState<BannerRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<BannerRecord | null>(null);
  const [uploading, setUploading] = useState(false);
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

  /** 图片上传:POST /upload/image 拿 URL 后写入表单 */
  const handleUpload = async (options: UploadRequestOption<File>) => {
    setUploading(true);
    try {
      const fd = new FormData();
      fd.append('file', options.file);
      const res = await request.post('/upload/image', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      const url = res.data?.data;
      if (url) {
        setForm((prev) => ({ ...prev, imageUrl: url }));
        message.success('图片上传成功');
      }
      options.onSuccess?.(url);
    } catch (e) {
      message.error('图片上传失败');
      options.onError?.(e instanceof Error ? e : new Error('上传失败'));
    } finally {
      setUploading(false);
    }
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
          {/* 图片上传(替代手动填 URL) */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ width: 160, height: 64, borderRadius: 6, overflow: 'hidden', background: token.colorFillQuaternary, display: 'flex', alignItems: 'center', justifyContent: 'center', border: `1px dashed ${token.colorBorderSecondary}` }}>
              {form.imageUrl ? <img src={form.imageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <span style={{ color: token.colorTextTertiary, fontSize: 12 }}>未上传</span>}
            </div>
            <Upload showUploadList={false} customRequest={handleUpload}>
              <Button icon={<UploadOutlined />} loading={uploading}>上传图片</Button>
            </Upload>
          </div>
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
