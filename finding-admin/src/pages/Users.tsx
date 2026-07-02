import { useEffect, useState } from 'react';
import { Table, Button, Input, Space, Tag, Popconfirm, Modal, Select, Upload, message } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface UserRecord {
  id: number; nickname: string; phone: string; school: string;
  status: number; realNameVerified: number; createdAt: string;
}

interface UserDetail {
  id: number; nickname: string; phone: string; avatar: string;
  school: string; gender: number; signature: string; city: string;
  status: number; role: string; realNameVerified: number;
}

const emptyForm = {
  nickname: '', phone: '', password: '', avatar: '',
  school: '', gender: 0, signature: '', city: '', status: 1,
};

export default function Users() {
  const [data, setData] = useState<UserRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');

  // 弹窗
  const [editOpen, setEditOpen] = useState(false);
  const [editLoading, setEditLoading] = useState(false);
  const [editingUser, setEditingUser] = useState<UserDetail | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [uploading, setUploading] = useState(false);

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

  const openCreate = () => {
    setEditingUser(null);
    setForm({ ...emptyForm });
    setEditOpen(true);
  };

  const openEdit = async (record: UserRecord) => {
    try {
      const res = await request.get(`/admin/users/${record.id}`);
      const u = res.data.data as UserDetail;
      setEditingUser(u);
      setForm({
        nickname: u.nickname || '',
        phone: u.phone || '',
        password: '',
        avatar: u.avatar || '',
        school: u.school || '',
        gender: u.gender ?? 0,
        signature: u.signature || '',
        city: u.city || '',
        status: u.status ?? 1,
      });
      setEditOpen(true);
    } catch { message.error('获取用户详情失败'); }
  };

  /** 头像上传 */
  const handleUpload = async (options: any) => {
    const file = options.file as File;
    setUploading(true);
    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await request.post('/upload/image', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      const url = res.data?.data;
      if (url) {
        setForm((prev) => ({ ...prev, avatar: url }));
        message.success('头像上传成功');
      }
      options.onSuccess?.(url);
    } catch {
      message.error('上传失败');
      options.onError?.({});
    }
    finally { setUploading(false); }
  };

  const handleSave = async () => {
    if (!form.nickname.trim() || !form.phone.trim()) {
      message.warning('昵称和手机号不能为空');
      return;
    }
    setEditLoading(true);
    try {
      const payload: Record<string, any> = { ...form };
      if (!payload.password) delete payload.password;
      if (editingUser) {
        await request.put(`/admin/users/${editingUser.id}`, payload);
        message.success('保存成功');
      } else {
        await request.post('/admin/users', payload);
        message.success('创建成功');
      }
      setEditOpen(false);
      fetchData(page);
    } catch (e: any) {
      message.error(e?.message || '操作失败');
    }
    finally { setEditLoading(false); }
  };

  const columns: ColumnsType<UserRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
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
          <a onClick={() => openEdit(record)}>编辑</a>
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
        <Button type="primary" onClick={openCreate}>+ 新建用户</Button>
      </Space>
      <Table
        columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{
          current: page, total, pageSize: 10,
          onChange: (p) => fetchData(p),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />

      {/* 新建/编辑用户弹窗 */}
      <Modal
        title={editingUser ? '编辑用户' : '新建用户'}
        open={editOpen}
        onOk={handleSave}
        onCancel={() => setEditOpen(false)}
        confirmLoading={editLoading}
        okText={editingUser ? '保存' : '创建'}
        width={520}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {/* 头像上传 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <div>
              {form.avatar ? (
                <img src={form.avatar} alt="" style={{ width: 64, height: 64, borderRadius: 8, objectFit: 'cover' }} />
              ) : (
                <div style={{ width: 64, height: 64, borderRadius: 8, background: '#f0f0f0', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#bbb' }}>头像</div>
              )}
            </div>
            <Upload
              showUploadList={false}
              customRequest={handleUpload}
            >
              <Button icon={<UploadOutlined />} loading={uploading}>上传头像</Button>
            </Upload>
          </div>

          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <label>昵称</label>
              <Input value={form.nickname} onChange={(e) => setForm({ ...form, nickname: e.target.value })} />
            </div>
            <div style={{ flex: 1 }}>
              <label>手机号</label>
              <Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </div>
          </div>

          <div>
            <label>{editingUser ? '新密码（留空不修改）' : '密码（默认 123456）'}</label>
            <Input.Password value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder={editingUser ? '留空则不修改密码' : '默认密码 123456'} />
          </div>

          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <label>学校</label>
              <Input value={form.school} onChange={(e) => setForm({ ...form, school: e.target.value })} />
            </div>
            <div style={{ flex: 1 }}>
              <label>城市</label>
              <Input value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
            </div>
          </div>

          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <label>性别</label>
              <Select
                value={form.gender}
                onChange={(v) => setForm({ ...form, gender: v })}
                style={{ width: '100%' }}
                options={[
                  { value: 0, label: '未知' },
                  { value: 1, label: '男' },
                  { value: 2, label: '女' },
                ]}
              />
            </div>
            <div style={{ flex: 1 }}>
              <label>状态</label>
              <Select
                value={form.status}
                onChange={(v) => setForm({ ...form, status: v })}
                style={{ width: '100%' }}
                options={[
                  { value: 1, label: '正常' },
                  { value: 0, label: '禁用' },
                  { value: 2, label: '冻结' },
                ]}
              />
            </div>
          </div>

          <div>
            <label>个性签名</label>
            <Input.TextArea rows={2} value={form.signature} onChange={(e) => setForm({ ...form, signature: e.target.value })} />
          </div>
        </div>
      </Modal>
    </div>
  );
}
