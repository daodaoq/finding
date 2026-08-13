import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Input, Popconfirm, Select, Modal, message, theme } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface PostRecord {
  id: number; content: string; userId: number; userNickname: string;
  likeCount: number; commentCount: number; status: number;
  isTop?: number; isHot?: number;
  reviewStatus?: number; reviewReason?: string; createdAt: string;
}

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '正常', color: 'success' },
  0: { label: '已删除', color: 'error' },
  2: { label: '已隐藏', color: 'warning' },
};

export default function Posts() {
  const { token } = theme.useToken();
  const [data, setData] = useState<PostRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');

  // 编辑弹窗
  const [editOpen, setEditOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState('');
  const [editLocation, setEditLocation] = useState('');

  const openEdit = (record: PostRecord) => {
    setEditingId(record.id);
    setEditContent(record.content);
    setEditLocation('');
    setEditOpen(true);
  };

  const handleSaveEdit = async () => {
    if (editingId == null) return;
    try {
      await request.put(`/admin/posts/${editingId}`, {
        content: editContent,
        location: editLocation || undefined,
      });
      message.success('已保存');
      setEditOpen(false);
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const fetchData = (p = 1, kw?: string) => {
    setLoading(true);
    request.get('/admin/posts', { params: { page: p, size: 10, keyword: kw || keyword } })
      .then((res) => {
        setData(res.data.data.records);
        setTotal(res.data.data.total);
        setPage(p);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1); }, []);

  const updateStatus = async (id: number, status: number) => {
    try {
      await request.put(`/admin/posts/${id}/status`, { status });
      message.success('已更新');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/admin/posts/${id}`);
      message.success('已删除');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const handleFlag = async (id: number, field: 'isTop' | 'isHot', value: number) => {
    try {
      await request.put(`/admin/posts/${id}/flag`, { [field]: value });
      message.success('已更新');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<PostRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '内容', dataIndex: 'content', ellipsis: true, render: (v: string, r: PostRecord) => (
      <span>
        {r.isTop === 1 && <Tag color="orange" style={{ marginRight: 4 }}>置顶</Tag>}
        {r.isHot === 1 && <Tag color="purple" style={{ marginRight: 4 }}>精华</Tag>}
        {v}
      </span>
    ) },
    { title: '发布者', dataIndex: 'userNickname', width: 100 },
    { title: '点赞', dataIndex: 'likeCount', width: 60 },
    { title: '评论', dataIndex: 'commentCount', width: 60 },
    {
      title: '状态', dataIndex: 'status', render: (v: number) => (
        <Tag color={STATUS_MAP[v]?.color}>{STATUS_MAP[v]?.label}</Tag>
      ),
    },
    {
      title: '审核', dataIndex: 'reviewStatus', width: 110, render: (v: number | undefined, record) => {
        const rv = v ?? 0;
        const tag = rv === 0 ? <Tag>已发布</Tag> : rv === 1 ? <Tag color="warning">待审</Tag> : <Tag color="error">已拒绝</Tag>;
        return <span>{tag}{rv === 2 && record.reviewReason ? <span style={{ fontSize: 11, color: token.colorTextTertiary }} title={record.reviewReason}>原因</span> : null}</span>;
      },
    },
    { title: '发布时间', dataIndex: 'createdAt', render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', render: (_, record) => (
        <Space>
          <a onClick={() => openEdit(record)}>编辑</a>
          {record.isTop === 1 ? (
            <a style={{ color: 'orange' }} onClick={() => handleFlag(record.id, 'isTop', 0)}>取消置顶</a>
          ) : (
            <a onClick={() => handleFlag(record.id, 'isTop', 1)}>置顶</a>
          )}
          {record.isHot === 1 ? (
            <a style={{ color: 'purple' }} onClick={() => handleFlag(record.id, 'isHot', 0)}>取消精华</a>
          ) : (
            <a onClick={() => handleFlag(record.id, 'isHot', 1)}>精华</a>
          )}
          {record.status !== 2 && (
            <Popconfirm title="确定隐藏该动态？" onConfirm={() => updateStatus(record.id, 2)}>
              <a>隐藏</a>
            </Popconfirm>
          )}
          {record.status === 2 && (
            <Popconfirm title="确定恢复该动态？" onConfirm={() => updateStatus(record.id, 1)}>
              <a style={{ color: 'green' }}>恢复</a>
            </Popconfirm>
          )}
          <Popconfirm title="确定删除该动态？" onConfirm={() => handleDelete(record.id)}>
            <a style={{ color: 'red' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>动态管理</h2>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="搜索内容或ID"
          style={{ width: 300 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={(v) => fetchData(1, v)}
        />
      </Space>
      <Table
        columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{
          current: page, total, pageSize: 10,
          onChange: (p) => fetchData(p),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />

      {/* 编辑动态弹窗 */}
      <Modal
        title="编辑动态"
        open={editOpen}
        onOk={handleSaveEdit}
        onCancel={() => setEditOpen(false)}
        okText="保存"
        width={560}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Input.TextArea
            rows={6}
            placeholder="动态内容"
            value={editContent}
            onChange={(e) => setEditContent(e.target.value)}
          />
          <Input
            placeholder="位置（选填）"
            value={editLocation}
            onChange={(e) => setEditLocation(e.target.value)}
          />
        </div>
      </Modal>
    </div>
  );
}
