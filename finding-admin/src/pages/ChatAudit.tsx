import { useEffect, useState } from 'react';
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

  // ── 用户列表(私聊审查) ──
  const [userList, setUserList] = useState<any[]>([]);
  const [userListLoading, setUserListLoading] = useState(false);
  const [userListPage, setUserListPage] = useState(1);
  const [userListTotal, setUserListTotal] = useState(0);
  const [userKeyword, setUserKeyword] = useState('');

  // ── 群列表(群聊审查) ──
  const [groupList, setGroupList] = useState<any[]>([]);
  const [groupListLoading, setGroupListLoading] = useState(false);
  const [groupListPage, setGroupListPage] = useState(1);
  const [groupListTotal, setGroupListTotal] = useState(0);
  const [groupKeyword, setGroupKeyword] = useState('');

  // ── 私聊消息 ──
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [selectedUser, setSelectedUser] = useState('');
  const [otherKeyword, setOtherKeyword] = useState('');
  const [otherOptions, setOtherOptions] = useState<any[]>([]);
  const [otherUserId, setOtherUserId] = useState<number | null>(null);
  const [data, setData] = useState<ChatMsg[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);

  // ── 群消息 ──
  const [selectedGroup, setSelectedGroup] = useState<{ id: number; name: string } | null>(null);
  const [gData, setGData] = useState<GroupMsg[]>([]);
  const [gLoading, setGLoading] = useState(false);
  const [gTotal, setGTotal] = useState(0);
  const [gPage, setGPage] = useState(1);

  useEffect(() => {
    fetchUserList(1);
    fetchGroupList(1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── 用户列表 ──
  const fetchUserList = (p = 1, kw?: string) => {
    setUserListLoading(true);
    request.get('/admin/users', { params: { page: p, size: 10, keyword: kw || userKeyword } })
      .then((res) => { setUserList(res.data.data.records); setUserListTotal(res.data.data.total); setUserListPage(p); })
      .catch(() => {})
      .finally(() => setUserListLoading(false));
  };

  const selectUser = (u: any) => {
    setSelectedUserId(u.id);
    setSelectedUser(u.nickname || `用户${u.id}`);
    setOtherUserId(null);
    setOtherOptions([]);
    setOtherKeyword('');
    setPage(1);
    fetchMessages(u.id, null, 1);
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

  // ── 群列表 ──
  const fetchGroupList = (p = 1, kw?: string) => {
    setGroupListLoading(true);
    request.get('/admin/groups', { params: { page: p, size: 10, keyword: kw || groupKeyword } })
      .then((res) => { setGroupList(res.data.data.records); setGroupListTotal(res.data.data.total); setGroupListPage(p); })
      .catch(() => {})
      .finally(() => setGroupListLoading(false));
  };

  const selectGroup = (g: any) => {
    setSelectedGroup({ id: g.id, name: g.name || `群${g.id}` });
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

  // ── 表格列 ──
  const userColumns: ColumnsType<any> = [
    { title: '序号', width: 70, render: (_, __, i) => (userListPage - 1) * 10 + i + 1 },
    { title: '昵称', dataIndex: 'nickname' },
    { title: '手机号', dataIndex: 'phone', width: 130 },
    { title: '学校', dataIndex: 'school' },
  ];

  const groupColumns: ColumnsType<any> = [
    { title: '序号', width: 70, render: (_, __, i) => (groupListPage - 1) * 10 + i + 1 },
    { title: '群名', dataIndex: 'name' },
    { title: '成员', dataIndex: 'memberCount', width: 80 },
    { title: '创建时间', dataIndex: 'createdAt', render: (v: string) => v?.replace('T', ' ') },
  ];

  const chatColumns: ColumnsType<ChatMsg> = [
    { title: '序号', width: 70, render: (_, __, i) => (page - 1) * 20 + i + 1 },
    { title: '发送方', dataIndex: 'fromUserId', width: 80 },
    { title: '接收方', dataIndex: 'toUserId', width: 80 },
    { title: '类型', dataIndex: 'messageType', width: 70, render: (v: string) => (v === 'image' ? '图片' : '文字') },
    { title: '内容', dataIndex: 'content', ellipsis: true, render: (v: string, r: ChatMsg) => <span>{recalledTag(r.isRecalled === 1)}{v}</span> },
    { title: '时间', dataIndex: 'createdAt', width: 160, render: (v: string) => v?.replace('T', ' ') },
    { title: '操作', render: (_, r) => (
      <Popconfirm title="确定删除这条消息？" onConfirm={() => handleDeleteChat(r.id)}>
        <a style={{ color: 'red' }}>删除</a>
      </Popconfirm>
    )},
  ];

  const groupMsgColumns: ColumnsType<GroupMsg> = [
    { title: '序号', width: 70, render: (_, __, i) => (gPage - 1) * 20 + i + 1 },
    { title: '群', dataIndex: 'groupName', width: 120 },
    { title: '发送人', dataIndex: 'senderName', width: 100, render: (v: string, r: GroupMsg) => <span>{v}（{r.fromUserId}）</span> },
    { title: '类型', dataIndex: 'messageType', width: 70, render: (v: string) => (v === 'image' ? '图片' : '文字') },
    { title: '内容', dataIndex: 'content', ellipsis: true, render: (v: string, r: GroupMsg) => <span>{recalledTag(r.isRecalled === 1)}{v}</span> },
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
              {/* 用户选择列表 */}
              <Space style={{ marginBottom: 12 }}>
                <Input.Search
                  placeholder="按昵称/手机号过滤"
                  style={{ width: 260 }}
                  value={userKeyword}
                  onChange={(e) => setUserKeyword(e.target.value)}
                  onSearch={(v) => fetchUserList(1, v)}
                  allowClear
                />
              </Space>
              <Table
                size="small"
                columns={userColumns}
                dataSource={userList}
                rowKey="id"
                loading={userListLoading}
                rowClassName="cursor-pointer"
                onRow={(r) => ({
                  onClick: () => selectUser(r),
                  style: { cursor: 'pointer', background: r.id === selectedUserId ? '#fff1f3' : undefined },
                })}
                pagination={{
                  current: userListPage, total: userListTotal, pageSize: 10, size: 'small',
                  onChange: (p) => fetchUserList(p),
                  showTotal: (t) => `共 ${t} 人`,
                }}
              />
              <p style={{ color: '#999', margin: '12px 0 4px', fontSize: 13 }}>
                👆 点击上方某个用户，查看 TA 的私聊（可再选「只看和谁的对话」）
              </p>
              {/* 私聊消息 */}
              {selectedUser && (
                <Space style={{ marginBottom: 12 }}>
                  <span style={{ fontWeight: 600 }}>用户「{selectedUser}」的私聊</span>
                  <Input.Search
                    placeholder="只看和谁的对话（选填）"
                    style={{ width: 220 }}
                    value={otherKeyword}
                    onChange={(e) => setOtherKeyword(e.target.value)}
                    onSearch={searchOther}
                  />
                </Space>
              )}
              {otherOptions.length > 0 && (
                <div style={{ marginBottom: 8 }}>
                  {otherOptions.map((u) => (
                    <a key={u.id} onClick={() => pickOther(u)} style={{ marginRight: 12 }}>{u.nickname}（{u.id}）</a>
                  ))}
                </div>
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
              {/* 群选择列表 */}
              <Space style={{ marginBottom: 12 }}>
                <Input.Search
                  placeholder="按群名过滤"
                  style={{ width: 260 }}
                  value={groupKeyword}
                  onChange={(e) => setGroupKeyword(e.target.value)}
                  onSearch={(v) => fetchGroupList(1, v)}
                  allowClear
                />
              </Space>
              <Table
                size="small"
                columns={groupColumns}
                dataSource={groupList}
                rowKey="id"
                loading={groupListLoading}
                onRow={(r) => ({
                  onClick: () => selectGroup(r),
                  style: { cursor: 'pointer', background: r.id === selectedGroup?.id ? '#fff1f3' : undefined },
                })}
                pagination={{
                  current: groupListPage, total: groupListTotal, pageSize: 10, size: 'small',
                  onChange: (p) => fetchGroupList(p),
                  showTotal: (t) => `共 ${t} 个群`,
                }}
              />
              <p style={{ color: '#999', margin: '12px 0 4px', fontSize: 13 }}>👆 点击上方某个群，查看群内全部消息</p>
              {selectedGroup && (
                <p style={{ fontWeight: 600, marginBottom: 8 }}>群「{selectedGroup.name}」的消息（撤回保留原文）</p>
              )}
              <Table
                columns={groupMsgColumns} dataSource={gData} rowKey="id" loading={gLoading}
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
