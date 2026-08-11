import { useEffect, useState } from 'react';
import { Table, Space, Tag, Modal, Input, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface AppealRecord {
  id: number;
  userId: number;
  userNickname: string;
  targetType?: string;
  targetId?: number;
  reason: string;
  originalResult?: string;
  status: number;
  handleNote?: string;
  createdAt: string;
}

export default function Appeals() {
  const [data, setData] = useState<AppealRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [activeTab, setActiveTab] = useState('all');
  const [detailTarget, setDetailTarget] = useState<AppealRecord | null>(null);
  const [rejectTarget, setRejectTarget] = useState<AppealRecord | null>(null);
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchData = (p = 1, status?: string) => {
    setLoading(true);
    request.get('/admin/appeals', {
      params: { page: p, size: 10, status: status === 'all' ? undefined : Number(status || activeTab) },
    })
      .then((res) => { setData(res.data.data.records); setTotal(res.data.data.total); setPage(p); })
      .catch(() => message.error('获取申诉失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1, activeTab); }, [activeTab]);

  const handleAppeal = async (id: number, pass: boolean) => {
    setSubmitting(true);
    try {
      await request.put(`/admin/appeals/${id}/handle`, { pass, note: note.trim() || undefined });
      message.success(pass ? '已通过并重新发布' : '已驳回');
      setRejectTarget(null);
      setNote('');
      fetchData(page, activeTab);
    } catch { message.error('操作失败'); }
    finally { setSubmitting(false); }
  };

  const columns: ColumnsType<AppealRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '申诉人', dataIndex: 'userNickname', width: 120 },
    { title: '申诉理由', dataIndex: 'reason', ellipsis: true },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (v: number) => v === 0 ? <Tag color="processing">待处理</Tag> : v === 1 ? <Tag color="success">已通过</Tag> : <Tag>已驳回</Tag>,
    },
    { title: '提交时间', dataIndex: 'createdAt', width: 160, render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', width: 160,
      render: (_, record) => (
        <Space>
          <a onClick={() => setDetailTarget(record)}>查看</a>
          {record.status === 0 && (
            <>
              <a style={{ color: '#52c41a' }} onClick={() => handleAppeal(record.id, true)}>通过</a>
              <a style={{ color: '#f5222d' }} onClick={() => setRejectTarget(record)}>驳回</a>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>申诉管理</h2>
      <div style={{ marginBottom: 12 }}>
        {['all', '0', '1', '2'].map((k) => (
          <Tag key={k} color={activeTab === k ? 'blue' : ''} style={{ cursor: 'pointer', marginRight: 8 }} onClick={() => setActiveTab(k)}>
            {k === 'all' ? '全部' : k === '0' ? '待处理' : k === '1' ? '已通过' : '已驳回'}
          </Tag>
        ))}
      </div>
      <Table
        columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{ current: page, total, pageSize: 10, onChange: (p) => fetchData(p, activeTab), showTotal: (t) => `共 ${t} 条` }}
      />

      {/* 详情 */}
      <Modal title="申诉详情" open={detailTarget != null} onCancel={() => setDetailTarget(null)} footer={null} width={480}>
        {detailTarget && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div><b>申诉人：</b>{detailTarget.userNickname}</div>
            <div><b>申诉理由：</b>{detailTarget.reason}</div>
            {detailTarget.originalResult && <div><b>原处理结果：</b>{detailTarget.originalResult}</div>}
            {detailTarget.handleNote && <div><b>处理意见：</b>{detailTarget.handleNote}</div>}
            <div><b>提交时间：</b>{detailTarget.createdAt?.replace('T', ' ')}</div>
          </div>
        )}
      </Modal>

      {/* 驳回弹窗 */}
      <Modal
        title="驳回申诉"
        open={rejectTarget != null}
        onOk={() => rejectTarget && handleAppeal(rejectTarget.id, false)}
        onCancel={() => setRejectTarget(null)}
        okText="确认驳回"
        okButtonProps={{ danger: true }}
        confirmLoading={submitting}
        width={440}
      >
        <Input.TextArea rows={3} placeholder="驳回意见（可选）" value={note} onChange={(e) => setNote(e.target.value)} />
      </Modal>
    </div>
  );
}
