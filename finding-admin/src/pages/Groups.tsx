import { useEffect, useState } from 'react';
import { Table, Space, Popconfirm, Input, Modal, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface GroupRecord {
  id: number;
  name: string;
  ownerId: number;
  ownerNickname: string;
  memberCount: number;
  avatar?: string;
  createdAt: string;
}

export default function Groups() {
  const [data, setData] = useState<GroupRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');

  // 编辑弹窗
  const [editOpen, setEditOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState({ name: '', announcement: '' });

  const openEdit = (record: GroupRecord) => {
    setEditingId(record.id);
    setEditForm({ name: record.name, announcement: '' });
    setEditOpen(true);
  };

  const handleSaveEdit = async () => {
    if (editingId == null) return;
    try {
      await request.put(`/admin/groups/${editingId}`, {
        name: editForm.name,
        announcement: editForm.announcement || undefined,
      });
      message.success('已保存');
      setEditOpen(false);
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const fetchData = (p = 1, kw?: string) => {
    setLoading(true);
    request.get('/admin/groups', { params: { page: p, size: 10, keyword: kw || keyword } })
      .then((res) => {
        setData(res.data.data.records);
        setTotal(res.data.data.total);
        setPage(p);
      })
      .catch(() => message.error('获取群聊列表失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1); }, []);

  const handleDisband = async (id: number) => {
    try {
      await request.delete(`/admin/groups/${id}`);
      message.success('已解散');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<GroupRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    {
      title: '群名', dataIndex: 'name',
      render: (v: string, r) => (
        <Space size={6}>
          {r.avatar ? <img src={r.avatar} alt="" style={{ width: 28, height: 28, borderRadius: 4, objectFit: 'cover' }} /> : null}
          <span>{v}</span>
        </Space>
      ),
    },
    { title: '群主', dataIndex: 'ownerNickname', width: 120 },
    { title: '成员数', dataIndex: 'memberCount', width: 80 },
    { title: '创建时间', dataIndex: 'createdAt', width: 160, render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', width: 120,
      render: (_, record) => (
        <Space>
          <a onClick={() => openEdit(record)}>编辑</a>
          <Popconfirm title={`确定解散群「${record.name}」？（将删除群消息和成员关系）`} onConfirm={() => handleDisband(record.id)}>
            <a style={{ color: 'red' }}>解散</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>群聊管理</h2>
      <Input.Search
        placeholder="搜索群名"
        style={{ width: 300, marginBottom: 16 }}
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        onSearch={(v) => fetchData(1, v)}
      />
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page, total, pageSize: 10,
          onChange: (p) => fetchData(p),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />

      {/* 编辑群聊弹窗 */}
      <Modal
        title="编辑群聊"
        open={editOpen}
        onOk={handleSaveEdit}
        onCancel={() => setEditOpen(false)}
        okText="保存"
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Input
            placeholder="群名"
            value={editForm.name}
            onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
          />
          <Input.TextArea
            rows={3}
            placeholder="群公告"
            value={editForm.announcement}
            onChange={(e) => setEditForm({ ...editForm, announcement: e.target.value })}
          />
        </div>
      </Modal>
    </div>
  );
}
