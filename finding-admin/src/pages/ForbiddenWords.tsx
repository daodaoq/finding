import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Popconfirm, Modal, Input, message, Switch, Radio, theme } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface ForbiddenWordRecord {
  id: number; word: string; status: number; action: number; createdAt: string;
}

export default function ForbiddenWords() {
  const { token } = theme.useToken();
  const [data, setData] = useState<ForbiddenWordRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<ForbiddenWordRecord | null>(null);
  const [form, setForm] = useState({ word: '', status: 1, action: 0 });

  const fetchData = (p = 1, kw?: string) => {
    setLoading(true);
    request.get('/admin/forbidden-words', { params: { page: p, size: 10, keyword: kw || keyword } })
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
    setForm({ word: '', status: 1, action: 0 });
    setModalOpen(true);
  };

  const openEdit = (record: ForbiddenWordRecord) => {
    setEditing(record);
    setForm({ word: record.word, status: record.status, action: record.action ?? 0 });
    setModalOpen(true);
  };

  const handleSave = async () => {
    if (!form.word.trim()) { message.warning('请输入违禁词'); return; }
    try {
      if (editing) {
        await request.put(`/admin/forbidden-words/${editing.id}`, { word: form.word.trim(), action: form.action });
        message.success('已更新');
      } else {
        await request.post('/admin/forbidden-words', { word: form.word.trim(), action: form.action });
        message.success('已新增');
      }
      setModalOpen(false);
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/admin/forbidden-words/${id}`);
      message.success('已删除');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const toggleStatus = async (record: ForbiddenWordRecord) => {
    const newStatus = record.status === 1 ? 0 : 1;
    try {
      await request.put(`/admin/forbidden-words/${record.id}/status`, { status: newStatus });
      message.success(newStatus === 1 ? '已启用' : '已禁用');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<ForbiddenWordRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '违禁词', dataIndex: 'word' },
    { title: '动作', dataIndex: 'action', width: 80, render: (v: number) => (
      <Tag color={v === 1 ? 'warning' : 'error'}>{v === 1 ? '送审' : '拦截'}</Tag>
    )},
    { title: '状态', dataIndex: 'status', render: (v: number) => (
      <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag>
    )},
    { title: '创建时间', dataIndex: 'createdAt', render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', render: (_, record) => (
        <Space>
          <a onClick={() => openEdit(record)}>编辑</a>
          <Popconfirm title={`确定${record.status === 1 ? '禁用' : '启用'}该词？`} onConfirm={() => toggleStatus(record)}>
            <a style={{ color: record.status === 1 ? 'orange' : 'green' }}>
              {record.status === 1 ? '禁用' : '启用'}
            </a>
          </Popconfirm>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <a style={{ color: 'red' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>违禁词管理</h2>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          placeholder="搜索违禁词"
          style={{ width: 300 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={(v) => fetchData(1, v)}
          allowClear
        />
        <Button type="primary" onClick={openCreate}>+ 新增违禁词</Button>
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
        title={editing ? '编辑违禁词' : '新增违禁词'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        okText={editing ? '保存' : '新增'}
        width={460}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Input
            placeholder="违禁词(中英文均可,保存后全站即时生效)"
            value={form.word}
            onChange={(e) => setForm({ ...form, word: e.target.value })}
            maxLength={100}
          />
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span>启用:</span>
            <Switch checked={form.status === 1} onChange={(v) => setForm({ ...form, status: v ? 1 : 0 })} />
            <span style={{ fontSize: 12, color: token.colorTextPlaceholder, marginLeft: 8 }}>
              禁用的词不会拦截用户内容
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span>动作:</span>
            <Radio.Group
              value={form.action}
              onChange={(e) => setForm({ ...form, action: e.target.value })}
              options={[
                { label: '拦截(命中即拒绝发布)', value: 0 },
                { label: '送审(命中进入审核队列)', value: 1 },
              ]}
            />
          </div>
        </div>
      </Modal>
    </div>
  );
}
