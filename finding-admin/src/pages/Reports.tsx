import { useEffect, useState } from 'react';
import { Table, Space, Tag, Tabs, Avatar, Modal, Input, InputNumber, message, theme } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface ReportRecord {
  id: number;
  fromUserId: number;
  fromNickname: string;
  fromAvatar?: string;
  targetUserId: number;
  targetNickname: string;
  targetAvatar?: string;
  reason: string;
  evidence?: string;
  status: number;
  handleBy?: number;
  handleNote?: string;
  handleTime?: string;
  roomId?: number;
  targetType?: string;
  targetId?: number;
  contentSnapshot?: string;
  createdAt: string;
  targetReportCount?: number;
  fromReportCount?: number;
}

const TYPE_LABEL: Record<string, string> = {
  message: '聊天消息',
  post: '动态',
  comment: '评论',
  mate: '搭子邀约',
  group: '群聊',
  user: '用户资料',
  resume: '情感简历/个人介绍',
};

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待处理', color: 'processing' },
  1: { label: '已处理', color: 'success' },
  2: { label: '已驳回', color: 'default' },
};

export default function Reports() {
  const { token } = theme.useToken();
  const [data, setData] = useState<ReportRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [activeTab, setActiveTab] = useState('all');

  const fetchData = (p = 1, status?: string) => {
    setLoading(true);
    request.get('/admin/reports', {
      params: {
        page: p,
        size: 10,
        status: status === 'all' ? undefined : Number(status || activeTab),
      },
    })
      .then((res) => {
        setData(res.data.data.records);
        setTotal(res.data.data.total);
        setPage(p);
      })
      .catch(() => message.error('获取投诉列表失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1, activeTab); }, [activeTab]);

  // 处理/驳回弹窗(带处理意见)
  const [handleTarget, setHandleTarget] = useState<{ id: number; action: 1 | 2; nickname: string } | null>(null);
  const [handleNote, setHandleNote] = useState('');
  const [handleLoading, setHandleLoading] = useState(false);

  const openHandle = (record: ReportRecord, action: 1 | 2) => {
    setHandleTarget({ id: record.id, action, nickname: record.fromNickname });
    setHandleNote('');
  };

  const confirmHandle = async () => {
    if (!handleTarget) return;
    setHandleLoading(true);
    try {
      await request.put(`/admin/reports/${handleTarget.id}/status`, {
        status: handleTarget.action,
        note: handleNote.trim() || undefined,
      });
      message.success(handleTarget.action === 1 ? '已处理并通知投诉人' : '已驳回并通知投诉人');
      setHandleTarget(null);
      fetchData(page, activeTab);
    } catch { message.error('操作失败'); }
    finally { setHandleLoading(false); }
  };

  // 详情弹窗
  const [detailTarget, setDetailTarget] = useState<ReportRecord | null>(null);

  // 封禁弹窗(可选天数 + 原因)
  const [banTarget, setBanTarget] = useState<{ id: number; nickname: string } | null>(null);
  const [banDays, setBanDays] = useState(0);
  const [banReason, setBanReason] = useState('');
  const [banLoading, setBanLoading] = useState(false);

  const openBan = (record: ReportRecord) => {
    setBanTarget({ id: record.targetUserId, nickname: record.targetNickname });
    setBanDays(0);
    setBanReason(record.reason || '');
  };

  const confirmBan = async () => {
    if (!banTarget) return;
    setBanLoading(true);
    try {
      await request.put(`/admin/users/${banTarget.id}/ban`, {
        days: banDays,
        reason: banReason.trim() || undefined,
      });
      message.success(banDays > 0 ? `已封禁 ${banTarget.nickname} ${banDays} 天` : `已永久封禁 ${banTarget.nickname}`);
      setBanTarget(null);
    } catch { message.error('封禁失败'); }
    finally { setBanLoading(false); }
  };

  const userCell = (nickname: string, avatar?: string) => (
    <Space size={6}>
      <Avatar size={24} src={avatar}>{nickname?.[0] || '?'}</Avatar>
      <span>{nickname}</span>
    </Space>
  );

  const columns: ColumnsType<ReportRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '投诉人', render: (_, r) => userCell(r.fromNickname, r.fromAvatar) },
    { title: '被投诉人', render: (_, r) => userCell(r.targetNickname, r.targetAvatar) },
    { title: '投诉原因', dataIndex: 'reason', ellipsis: true },
    {
      title: '投诉类型', dataIndex: 'targetType', width: 90,
      render: (v?: string) => (v ? TYPE_LABEL[v] || v : '—'),
    },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (v: number) => <Tag color={STATUS_MAP[v]?.color}>{STATUS_MAP[v]?.label}</Tag>,
    },
    { title: '时间', dataIndex: 'createdAt', width: 160, render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', width: 240,
      render: (_, record) => (
        <Space>
          <a onClick={() => setDetailTarget(record)}>查看</a>
          {record.status === 0 && (
            <>
              <a onClick={() => openHandle(record, 1)}>处理</a>
              <a style={{ color: token.colorTextTertiary }} onClick={() => openHandle(record, 2)}>驳回</a>
            </>
          )}
          <a style={{ color: 'red' }} onClick={() => openBan(record)}>封禁</a>
        </Space>
      ),
    },
  ];

  const tabItems = [
    { key: 'all', label: '全部' },
    { key: '0', label: '待处理' },
    { key: '1', label: '已处理' },
    { key: '2', label: '已驳回' },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>投诉管理</h2>
      <Tabs activeKey={activeTab} onChange={(k) => setActiveTab(k)} items={tabItems} />
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

      {/* 封禁弹窗 */}
      <Modal
        title={`封禁 ${banTarget?.nickname ?? ''}`}
        open={banTarget != null}
        onOk={confirmBan}
        onCancel={() => setBanTarget(null)}
        okText="确认封禁"
        okButtonProps={{ danger: true }}
        confirmLoading={banLoading}
        width={420}
      >
        <p style={{ marginBottom: 12 }}>封禁天数：</p>
        <InputNumber
          min={0}
          max={3650}
          value={banDays}
          onChange={(v) => setBanDays(v ?? 0)}
          style={{ width: '100%' }}
          addonAfter="天"
        />
        <p style={{ marginTop: 12, marginBottom: 8 }}>封禁原因：</p>
        <Input.TextArea
          rows={2}
          placeholder="填写的封禁原因会展示给用户"
          value={banReason}
          onChange={(e) => setBanReason(e.target.value)}
        />
        <p style={{ marginTop: 12, fontSize: 12, color: token.colorTextTertiary }}>
          0 表示永久封禁；按天封禁到期后账号自动解封。封禁立即生效，已登录也会被强制失效。
        </p>
      </Modal>

      {/* 处理/驳回弹窗(带处理意见) */}
      <Modal
        title={handleTarget ? (handleTarget.action === 1 ? '处理投诉' : '驳回投诉') : ''}
        open={handleTarget != null}
        onOk={confirmHandle}
        onCancel={() => setHandleTarget(null)}
        okText={handleTarget?.action === 1 ? '确认处理' : '确认驳回'}
        okButtonProps={handleTarget?.action === 2 ? { danger: true } : undefined}
        confirmLoading={handleLoading}
        width={420}
      >
        <p style={{ marginBottom: 8 }}>处理对象：{handleTarget?.nickname}</p>
        <Input.TextArea
          rows={3}
          placeholder="处理意见（选填，会通知给投诉人）"
          value={handleNote}
          onChange={(e) => setHandleNote(e.target.value)}
        />
      </Modal>

      {/* 投诉详情弹窗 */}
      <Modal
        title="投诉详情"
        open={detailTarget != null}
        onCancel={() => setDetailTarget(null)}
        footer={null}
        width={520}
      >
        {detailTarget && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div><b>投诉人：</b>{detailTarget.fromNickname}
              {detailTarget.fromReportCount != null && detailTarget.fromReportCount > 0 && (
                <Tag color="orange" style={{ marginLeft: 8 }}>累计投诉 {detailTarget.fromReportCount} 次</Tag>
              )}
            </div>
            <div><b>被投诉人：</b>{detailTarget.targetNickname}
              {detailTarget.targetReportCount != null && detailTarget.targetReportCount > 0 && (
                <Tag color="red" style={{ marginLeft: 8 }}>累计被投诉 {detailTarget.targetReportCount} 次</Tag>
              )}
            </div>
            <div><b>投诉类型：</b>{detailTarget.targetType ? TYPE_LABEL[detailTarget.targetType] || detailTarget.targetType : '用户'}</div>
            <div><b>投诉原因：</b>{detailTarget.reason}</div>
            <div><b>提交时间：</b>{detailTarget.createdAt?.replace('T', ' ')}</div>

            {/* 证据图片 */}
            {detailTarget.evidence ? (
              <div>
                <div style={{ fontWeight: 600, marginBottom: 6 }}>证据附件：</div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                  {detailTarget.evidence.split(',').filter(Boolean).map((url, i) => (
                    <img key={i} src={url} alt="证据" style={{ width: 80, height: 80, objectFit: 'cover', borderRadius: 8, border: `1px solid ${token.colorBorderSecondary}` }} />
                  ))}
                </div>
              </div>
            ) : null}

            <div style={{ marginTop: 8 }}>
              <div style={{ fontWeight: 600, marginBottom: 6 }}>被投诉内容：</div>
              <div style={{
                background: token.colorFillQuaternary, borderRadius: 8, padding: '10px 12px',
                whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontSize: 13, lineHeight: 1.7, color: token.colorTextSecondary,
              }}>
                {detailTarget.contentSnapshot || '（旧投诉无内容快照）'}
              </div>
            </div>

            {/* 处理记录 */}
            {detailTarget.status !== 0 && (
              <div style={{ marginTop: 8, paddingTop: 10, borderTop: `1px solid ${token.colorFillSecondary}`, fontSize: 13, color: token.colorTextSecondary }}>
                <div><b>处理结果：</b>{detailTarget.status === 1 ? '已处理' : '已驳回'}</div>
                {detailTarget.handleNote && <div><b>处理意见：</b>{detailTarget.handleNote}</div>}
                <div><b>处理时间：</b>{detailTarget.handleTime?.replace('T', ' ')}</div>
                <div><b>处理人ID：</b>{detailTarget.handleBy ?? '—'}</div>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
