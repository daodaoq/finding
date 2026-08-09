import { useEffect, useState } from 'react';
import { Table, Space, Popconfirm, Input, Modal, message, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface CommentRecord {
  id: number;
  postId: number;
  postContent: string;
  authorNickname: string;
  content: string;
  likeCount: number;
  parentId?: number;
  createdAt: string;
}

export default function Comments() {
  const [data, setData] = useState<CommentRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');

  // 编辑弹窗
  const [editOpen, setEditOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState('');

  const openEdit = (record: CommentRecord) => {
    setEditingId(record.id);
    setEditContent(record.content);
    setEditOpen(true);
  };

  const handleSaveEdit = async () => {
    if (editingId == null) return;
    try {
      await request.put(`/admin/comments/${editingId}`, { content: editContent });
      message.success('已保存');
      setEditOpen(false);
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const fetchData = (p = 1, kw?: string) => {
    setLoading(true);
    request.get('/admin/comments', { params: { page: p, size: 10, keyword: kw || keyword } })
      .then((res) => {
        setData(res.data.data.records);
        setTotal(res.data.data.total);
        setPage(p);
      })
      .catch(() => message.error('获取评论列表失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1); }, []);

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/admin/comments/${id}`);
      message.success('已删除');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<CommentRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '评论内容', dataIndex: 'content', ellipsis: true },
    { title: '作者', dataIndex: 'authorNickname', width: 110 },
    {
      title: '所属动态', dataIndex: 'postContent', width: 240, ellipsis: true,
      render: (v: string, r) => (
        <Tooltip title={`动态 #${r.postId}`}>
          <span>{v || `动态 #${r.postId}`}</span>
        </Tooltip>
      ),
    },
    { title: '点赞', dataIndex: 'likeCount', width: 70 },
    { title: '时间', dataIndex: 'createdAt', width: 160, render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', width: 120,
      render: (_, record) => (
        <Space>
          <a onClick={() => openEdit(record)}>编辑</a>
          <Popconfirm title="确定删除该评论？" onConfirm={() => handleDelete(record.id)}>
            <a style={{ color: 'red' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>评论管理</h2>
      <Input.Search
        placeholder="搜索评论内容"
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

      {/* 编辑评论弹窗 */}
      <Modal
        title="编辑评论"
        open={editOpen}
        onOk={handleSaveEdit}
        onCancel={() => setEditOpen(false)}
        okText="保存"
      >
        <Input.TextArea
          rows={4}
          value={editContent}
          onChange={(e) => setEditContent(e.target.value)}
        />
      </Modal>
    </div>
  );
}
