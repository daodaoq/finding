import { useState } from 'react';
import { Table, Button, Input, Space, Tag, Popconfirm, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface ChatMsg {
  id: number; fromUserId: number; toUserId: number;
  content: string; messageType: string; isRecalled?: number; createdAt: string;
}

export default function ChatAudit() {
  const [keyword, setKeyword] = useState('');
  const [userOptions, setUserOptions] = useState<any[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [selectedUser, setSelectedUser] = useState('');
  const [data, setData] = useState<ChatMsg[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);

  const searchUsers = async (kw: string) => {
    if (!kw.trim()) { message.warning('请输入用户昵称/手机号'); return; }
    const res = await request.get('/admin/users', { params: { page: 1, size: 10, keyword: kw.trim() } });
    const records = res.data?.data?.records || [];
    if (records.length === 0) { message.info('未找到用户'); setUserOptions([]); return; }
    setUserOptions(records);
  };

  const pickUser = (u: any) => {
    setSelectedUserId(u.id);
    setSelectedUser(u.nickname || `用户${u.id}`);
    setUserOptions([]);
    setPage(1);
    fetchMessages(u.id, 1);
  };

  const fetchMessages = (userId: number, p = 1) => {
    setLoading(true);
    request.get('/admin/messages/chat', { params: { userId, page: p, size: 20 } })
      .then((res) => { setData(res.data.data.records); setTotal(res.data.data.total); setPage(p); })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/admin/messages/chat/${id}`);
      message.success('已删除');
      if (selectedUserId) fetchMessages(selectedUserId, page);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<ChatMsg> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '发送方', dataIndex: 'fromUserId', width: 80 },
    { title: '接收方', dataIndex: 'toUserId', width: 80 },
    { title: '类型', dataIndex: 'messageType', width: 70, render: (v: string) => (v === 'image' ? '图片' : '文字') },
    { title: '内容', dataIndex: 'content', ellipsis: true, render: (v: string, r: ChatMsg) => r.isRecalled === 1 ? <Tag color="default">已撤回</Tag> : v },
    { title: '时间', dataIndex: 'createdAt', render: (v: string) => v?.replace('T', ' ') },
    { title: '操作', render: (_, r) => (
      <Popconfirm title="确定删除这条消息？" onConfirm={() => handleDelete(r.id)}>
        <a style={{ color: 'red' }}>删除</a>
      </Popconfirm>
    )},
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>聊天内容审查</h2>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          placeholder="搜索用户昵称/手机号"
          style={{ width: 300 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={searchUsers}
        />
      </Space>
      {userOptions.length > 0 && (
        <div style={{ marginBottom: 16 }}>
          {userOptions.map((u) => (
            <Button key={u.id} type="link" onClick={() => pickUser(u)} style={{ marginRight: 8 }}>
              {u.nickname}（{u.id}）
            </Button>
          ))}
        </div>
      )}
      {selectedUser && (
        <p style={{ color: '#666', marginBottom: 8 }}>
          查看用户「{selectedUser}」的私聊消息（含双方收发），可删除违规单条
        </p>
      )}
      <Table
        columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{
          current: page, total, pageSize: 20,
          onChange: (p) => selectedUserId && fetchMessages(selectedUserId, p),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />
    </div>
  );
}
