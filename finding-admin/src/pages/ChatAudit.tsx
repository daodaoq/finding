import { useState } from 'react';
import { Table, Input, Space, Tag, Popconfirm, message, Tabs } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface ChatMsg {
  id: number; fromUserId: number; toUserId: number;
  content: string; messageType: string; isRecalled?: number; createdAt: string;
}

interface GroupMsg {
  id: number; groupId: number; groupName: string; fromUserId: number; senderName: string;
  content: string; messageType: string; isRecalled?: number; createdAt: string;
}

const recalledTag = (r: boolean) => (r ? <Tag color="orange">已撤回</Tag> : null);

export default function ChatAudit() {
  const [tab, setTab] = useState('private');

  // ── 私聊审查状态 ──
  const [keyword, setKeyword] = useState('');
  const [userOptions, setUserOptions] = useState<any[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [selectedUser, setSelectedUser] = useState('');
  const [otherKeyword, setOtherKeyword] = useState('');
  const [otherOptions, setOtherOptions] = useState<any[]>([]);
  const [otherUserId, setOtherUserId] = useState<number | null>(null);
  const [data, setData] = useState<ChatMsg[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);

  // ── 群聊审查状态 ──
  const [gKeyword, setGKeyword] = useState('');
  const [groupOptions, setGroupOptions] = useState<any[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<{ id: number; name: string } | null>(null);
  const [gData, setGData] = useState<GroupMsg[]>([]);
  const [gLoading, setGLoading] = useState(false);
  const [gTotal, setGTotal] = useState(0);
  const [gPage, setGPage] = useState(1);

  // ── 私聊 ──
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
    fetchMessages(u.id, otherUserId, 1);
  };

  const searchOther = async (kw: string) => {
    if (!kw.trim()) return;
    const res = await request.get('/admin/users', { params: { page: 1, size: 10, keyword: kw.trim() } });
    setOtherOptions(res.data?.data?.records || []);
  };

  const pickOther = (u: any) => {
    setOtherUserId(u.id);
    setOtherOptions([]);
    setPage(1);
    if (selectedUserId) fetchMessages(selectedUserId, u.id, 1);
  };

  const fetchMessages = (userId: number, other: number | null, p = 1) => {
    setLoading(true);
    request.get('/admin/messages/chat', { params: { userId, otherUserId: other ?? undefined, page: p, size: 20 } })
      .then((res) => { setData(res.data.data.records); setTotal(res.data.data.total); setPage(p); })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  const handleDeleteChat = async (id: number) => {
    try {
      await request.delete(`/admin/messages/chat/${id}`);
      message.success('已删除');
      if (selectedUserId) fetchMessages(selectedUserId, otherUserId, page);
    } catch { message.error('操作失败'); }
  };

  // ── 群聊 ──
  const searchGroups = async (kw: string) => {
    if (!kw.trim()) { message.warning('请输入群名关键词'); return; }
    const res = await request.get('/admin/groups', { params: { page: 1, size: 10, keyword: kw.trim() } });
    const records = res.data?.data?.records || [];
    if (records.length === 0) { message.info('未找到群聊'); setGroupOptions([]); return; }
    setGroupOptions(records);
  };

  const pickGroup = (g: any) => {
    setSelectedGroup({ id: g.id, name: g.name || `群${g.id}` });
    setGroupOptions([]);
    setGPage(1);
    fetchGroupMessages(g.id, 1);
  };

  const fetchGroupMessages = (groupId: number, p = 1) => {
    setGLoading(true);
    request.get('/admin/messages/group', { params: { groupId, page: p, size: 20 } })
      .then((res) => { setGData(res.data.data.records); setGTotal(res.data.data.total); setGPage(p); })
      .catch(() => {})
      .finally(() => setGLoading(false));
  };

  const handleDeleteGroup = async (id: number) => {
    try {
      await request.delete(`/admin/messages/group/${id}`);
      message.success('已删除');
      if (selectedGroup) fetchGroupMessages(selectedGroup.id, gPage);
    } catch { message.error('操作失败'); }
  };

  const chatColumns: ColumnsType<ChatMsg> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '发送方', dataIndex: 'fromUserId', width: 80 },
    { title: '接收方', dataIndex: 'toUserId', width: 80 },
    { title: '类型', dataIndex: 'messageType', width: 70, render: (v: string) => (v === 'image' ? '图片' : '文字') },
    {
      title: '内容', dataIndex: 'content', ellipsis: true,
      render: (v: string, r: ChatMsg) => <span>{recalledTag(r.isRecalled === 1)}{v}</span>,
    },
    { title: '时间', dataIndex: 'createdAt', width: 160, render: (v: string) => v?.replace('T', ' ') },
    { title: '操作', render: (_, r) => (
      <Popconfirm title="确定删除这条消息？" onConfirm={() => handleDeleteChat(r.id)}>
        <a style={{ color: 'red' }}>删除</a>
      </Popconfirm>
    )},
  ];

  const groupColumns: ColumnsType<GroupMsg> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '群', dataIndex: 'groupName', width: 120 },
    { title: '发送人', dataIndex: 'senderName', width: 100, render: (v: string, r: GroupMsg) => <span>{v}（{r.fromUserId}）</span> },
    { title: '类型', dataIndex: 'messageType', width: 70, render: (v: string) => (v === 'image' ? '图片' : '文字') },
    {
      title: '内容', dataIndex: 'content', ellipsis: true,
      render: (v: string, r: GroupMsg) => <span>{recalledTag(r.isRecalled === 1)}{v}</span>,
    },
    { title: '时间', dataIndex: 'createdAt', width: 160, render: (v: string) => v?.replace('T', ' ') },
    { title: '操作', render: (_, r) => (
      <Popconfirm title="确定删除这条消息？" onConfirm={() => handleDeleteGroup(r.id)}>
        <a style={{ color: 'red' }}>删除</a>
      </Popconfirm>
    )},
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>聊天内容审查</h2>
      <Tabs activeKey={tab} onChange={setTab} items={[
        {
          key: 'private',
          label: '私聊审查',
          children: (
            <div>
              <Space style={{ marginBottom: 16 }} wrap>
                <Input.Search
                  placeholder="搜索用户昵称/手机号"
                  style={{ width: 260 }}
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  onSearch={searchUsers}
                />
                {selectedUserId != null && (
                  <Input.Search
                    placeholder="只看和谁的对话（选填）"
                    style={{ width: 220 }}
                    value={otherKeyword}
                    onChange={(e) => setOtherKeyword(e.target.value)}
                    onSearch={searchOther}
                  />
                )}
              </Space>
              {userOptions.length > 0 && (
                <div style={{ marginBottom: 8 }}>
                  {userOptions.map((u) => (
                    <a key={u.id} onClick={() => pickUser(u)} style={{ marginRight: 12 }}>{u.nickname}（{u.id}）</a>
                  ))}
                </div>
              )}
              {otherOptions.length > 0 && (
                <div style={{ marginBottom: 8 }}>
                  {otherOptions.map((u) => (
                    <a key={u.id} onClick={() => pickOther(u)} style={{ marginRight: 12 }}>{u.nickname}（{u.id}）</a>
                  ))}
                </div>
              )}
              {selectedUser && (
                <p style={{ color: '#666', marginBottom: 8 }}>
                  用户「{selectedUser}」的私聊{otherUserId ? ' 与对方' : ''}（含收发，撤回也保留原文）
                </p>
              )}
              <Table
                columns={chatColumns} dataSource={data} rowKey="id" loading={loading}
                pagination={{
                  current: page, total, pageSize: 20,
                  onChange: (p) => selectedUserId && fetchMessages(selectedUserId, otherUserId, p),
                  showTotal: (t) => `共 ${t} 条`,
                }}
              />
            </div>
          ),
        },
        {
          key: 'group',
          label: '群聊审查',
          children: (
            <div>
              <Space style={{ marginBottom: 16 }}>
                <Input.Search
                  placeholder="搜索群名"
                  style={{ width: 260 }}
                  value={gKeyword}
                  onChange={(e) => setGKeyword(e.target.value)}
                  onSearch={searchGroups}
                />
              </Space>
              {groupOptions.length > 0 && (
                <div style={{ marginBottom: 8 }}>
                  {groupOptions.map((g) => (
                    <a key={g.id} onClick={() => pickGroup(g)} style={{ marginRight: 12 }}>{g.name}（{g.id}）</a>
                  ))}
                </div>
              )}
              {selectedGroup && (
                <p style={{ color: '#666', marginBottom: 8 }}>群「{selectedGroup.name}」的全部消息（撤回保留原文）</p>
              )}
              <Table
                columns={groupColumns} dataSource={gData} rowKey="id" loading={gLoading}
                pagination={{
                  current: gPage, total: gTotal, pageSize: 20,
                  onChange: (p) => selectedGroup && fetchGroupMessages(selectedGroup.id, p),
                  showTotal: (t) => `共 ${t} 条`,
                }}
              />
            </div>
          ),
        },
      ]} />
    </div>
  );
}
