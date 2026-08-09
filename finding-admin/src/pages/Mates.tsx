import { useEffect, useState } from 'react';
import { Table, Space, Tag, Tabs, Popconfirm, Input, Modal, Select, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface MateRecord {
  id: number;
  title: string;
  category: string;
  categoryLabel: string;
  creatorNickname: string;
  maxParticipants: number;
  currentParticipants: number;
  activityTime?: string;
  location?: string;
  status: number;
  createdAt: string;
}

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '已下架', color: 'error' },
  1: { label: '进行中', color: 'success' },
  2: { label: '已结束', color: 'default' },
};

export default function Mates() {
  const [data, setData] = useState<MateRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [activeTab, setActiveTab] = useState('all');
  const [keyword, setKeyword] = useState('');

  // 编辑弹窗
  const [editOpen, setEditOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState({
    title: '', description: '', category: '', location: '',
    activityTime: '', maxParticipants: 10,
  });

  const openEdit = (record: MateRecord) => {
    setEditingId(record.id);
    setEditForm({
      title: record.title || '',
      description: '',
      category: record.category || '',
      location: record.location || '',
      activityTime: record.activityTime || '',
      maxParticipants: record.maxParticipants || 10,
    });
    setEditOpen(true);
  };

  const handleSaveEdit = async () => {
    if (editingId == null) return;
    try {
      await request.put(`/admin/mates/${editingId}`, {
        ...editForm,
        activityTime: editForm.activityTime || undefined,
      });
      message.success('已保存');
      setEditOpen(false);
      fetchData(page, activeTab);
    } catch { message.error('操作失败'); }
  };

  const fetchData = (p = 1, status?: string, kw?: string) => {
    setLoading(true);
    request.get('/admin/mates', {
      params: {
        page: p,
        size: 10,
        status: status === 'all' ? undefined : Number(status),
        keyword: kw || keyword,
      },
    })
      .then((res) => {
        setData(res.data.data.records);
        setTotal(res.data.data.total);
        setPage(p);
      })
      .catch(() => message.error('获取搭子列表失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1, activeTab); }, [activeTab]);

  const takeDown = async (id: number) => {
    try {
      await request.put(`/admin/mates/${id}/status`, { status: 0 });
      message.success('已下架');
      fetchData(page, activeTab);
    } catch { message.error('操作失败'); }
  };

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/admin/mates/${id}`);
      message.success('已删除');
      fetchData(page, activeTab);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<MateRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    { title: '分类', dataIndex: 'categoryLabel', width: 100 },
    { title: '发起人', dataIndex: 'creatorNickname', width: 110 },
    {
      title: '人数', width: 80,
      render: (_, r) => `${r.currentParticipants}/${r.maxParticipants}`,
    },
    {
      title: '活动时间', dataIndex: 'activityTime', width: 150,
      render: (v?: string) => v ? v.replace('T', ' ') : '待定',
    },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (v: number) => <Tag color={STATUS_MAP[v]?.color}>{STATUS_MAP[v]?.label}</Tag>,
    },
    {
      title: '操作', width: 180,
      render: (_, record) => (
        <Space>
          <a onClick={() => openEdit(record)}>编辑</a>
          {record.status === 1 && (
            <Popconfirm title="确定下架该邀约？" onConfirm={() => takeDown(record.id)}>
              <a style={{ color: '#fa8c16' }}>下架</a>
            </Popconfirm>
          )}
          <Popconfirm title="确定删除该邀约？（将同时清理报名记录）" onConfirm={() => handleDelete(record.id)}>
            <a style={{ color: 'red' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const tabItems = [
    { key: 'all', label: '全部' },
    { key: '1', label: '进行中' },
    { key: '0', label: '已下架' },
    { key: '2', label: '已结束' },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>搭子管理</h2>
      <Tabs activeKey={activeTab} onChange={(k) => setActiveTab(k)} items={tabItems} />
      <Input.Search
        placeholder="搜索标题或描述"
        style={{ width: 300, marginBottom: 16 }}
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        onSearch={(v) => fetchData(1, activeTab, v)}
      />
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page, total, pageSize: 10,
          onChange: (p) => fetchData(p, activeTab),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />

      {/* 编辑搭子弹窗 */}
      <Modal
        title="编辑搭子邀约"
        open={editOpen}
        onOk={handleSaveEdit}
        onCancel={() => setEditOpen(false)}
        okText="保存"
        width={520}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Input
            placeholder="标题"
            value={editForm.title}
            onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
          />
          <Input.TextArea
            rows={3}
            placeholder="描述"
            value={editForm.description}
            onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
          />
          <div style={{ display: 'flex', gap: 12 }}>
            <Select
              style={{ flex: 1 }}
              placeholder="分类"
              value={editForm.category || undefined}
              onChange={(v) => setEditForm({ ...editForm, category: v })}
              options={[
                { value: 'travel', label: '旅游' }, { value: 'carpool', label: '拼车' },
                { value: 'fitness', label: '健身' }, { value: 'study', label: '学习' },
                { value: 'exam', label: '备考' }, { value: 'sports', label: '运动' },
                { value: 'gaming', label: '游戏' }, { value: 'entertainment', label: '娱乐' },
                { value: 'other', label: '其他' },
              ]}
            />
            <Input
              type="number"
              placeholder="人数上限"
              style={{ width: 140 }}
              value={editForm.maxParticipants}
              onChange={(e) => setEditForm({ ...editForm, maxParticipants: Number(e.target.value) })}
            />
          </div>
          <Input
            placeholder="地点"
            value={editForm.location}
            onChange={(e) => setEditForm({ ...editForm, location: e.target.value })}
          />
          <Input
            type="datetime-local"
            value={editForm.activityTime}
            onChange={(e) => setEditForm({ ...editForm, activityTime: e.target.value })}
          />
        </div>
      </Modal>
    </div>
  );
}
