import { useEffect, useState } from 'react';
import { Table, Space, Tag, Modal, Input, Button, Popconfirm, message, theme } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface ReviewItem {
  id: number;
  content: string;
  images?: string[];
  userId: number;
  userNickname: string;
  createdAt: string;
}

export default function PostReview() {
  const { token } = theme.useToken();
  const [data, setData] = useState<ReviewItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [detailTarget, setDetailTarget] = useState<ReviewItem | null>(null);
  const [rejectTarget, setRejectTarget] = useState<ReviewItem | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [selectedKeys, setSelectedKeys] = useState<number[]>([]);
  const [batchMode, setBatchMode] = useState<'pass' | 'reject' | null>(null);
  const [batchReason, setBatchReason] = useState('');
  const [batchLoading, setBatchLoading] = useState(false);

  const fetchData = (p = 1) => {
    setLoading(true);
    request.get('/admin/posts/review', { params: { page: p, size: 10 } })
      .then((res) => {
        setData(res.data.data.records);
        setTotal(res.data.data.total);
        setPage(p);
      })
      .catch(() => message.error('获取待审动态失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1); }, []);

  const approve = async (id: number) => {
    try {
      await request.put(`/admin/posts/${id}/review`, { pass: true });
      message.success('已通过并发布');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const confirmReject = async () => {
    if (!rejectTarget) return;
    setSubmitting(true);
    try {
      await request.put(`/admin/posts/${rejectTarget.id}/review`, {
        pass: false,
        reason: rejectReason.trim() || undefined,
      });
      message.success('已拒绝并通知作者');
      setRejectTarget(null);
      setRejectReason('');
      fetchData(page);
    } catch { message.error('操作失败'); }
    finally { setSubmitting(false); }
  };

  /** 批量处理:勾选条目 通过/拒绝 */
  const confirmBatch = async () => {
    if (!batchMode || selectedKeys.length === 0) return;
    setBatchLoading(true);
    try {
      await request.post('/admin/posts/review/batch', {
        ids: selectedKeys,
        pass: batchMode === 'pass',
        reason: batchMode === 'reject' ? (batchReason.trim() || undefined) : undefined,
      });
      message.success(batchMode === 'pass' ? `已通过 ${selectedKeys.length} 条` : `已拒绝 ${selectedKeys.length} 条`);
      setSelectedKeys([]);
      setBatchMode(null);
      setBatchReason('');
      fetchData(page);
    } catch { message.error('操作失败'); }
    finally { setBatchLoading(false); }
  };

  const columns: ColumnsType<ReviewItem> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '动态内容', dataIndex: 'content', ellipsis: true },
    { title: '作者', dataIndex: 'userNickname', width: 120 },
    { title: '提交时间', dataIndex: 'createdAt', width: 160, render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', width: 160,
      render: (_, record) => (
        <Space>
          <a onClick={() => setDetailTarget(record)}>查看</a>
          <a style={{ color: token.colorSuccess }} onClick={() => approve(record.id)}>通过</a>
          <a style={{ color: token.colorError }} onClick={() => setRejectTarget(record)}>拒绝</a>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>动态审核队列</h2>
      <Space style={{ marginBottom: 12 }}>
        <Button type="primary" size="small" disabled={selectedKeys.length === 0} onClick={() => setBatchMode('pass')}>
          批量通过{selectedKeys.length ? `(${selectedKeys.length})` : ''}
        </Button>
        <Button danger size="small" disabled={selectedKeys.length === 0} onClick={() => setBatchMode('reject')}>
          批量拒绝{selectedKeys.length ? `(${selectedKeys.length})` : ''}
        </Button>
        {selectedKeys.length > 0 && (
          <Button size="small" onClick={() => setSelectedKeys([])}>清除选择</Button>
        )}
      </Space>
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        rowSelection={{
          selectedRowKeys: selectedKeys,
          onChange: (keys) => setSelectedKeys(keys as number[]),
        }}
        pagination={{
          current: page, total, pageSize: 10,
          onChange: (p) => fetchData(p),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />

      {/* 批量处理弹窗 */}
      <Modal
        title={batchMode === 'pass' ? '批量通过' : '批量拒绝'}
        open={batchMode != null}
        onOk={confirmBatch}
        onCancel={() => setBatchMode(null)}
        okText={batchMode === 'pass' ? '确认通过' : '确认拒绝'}
        okButtonProps={batchMode === 'reject' ? { danger: true } : undefined}
        confirmLoading={batchLoading}
        width={460}
      >
        {batchMode === 'reject' && (
          <Input.TextArea
            rows={3}
            placeholder="拒绝原因（会通知给各作者）"
            value={batchReason}
            onChange={(e) => setBatchReason(e.target.value)}
          />
        )}
      </Modal>

      {/* 拒绝原因弹窗 */}
      <Modal
        title="拒绝该动态"
        open={rejectTarget != null}
        onOk={confirmReject}
        onCancel={() => setRejectTarget(null)}
        okText="确认拒绝"
        okButtonProps={{ danger: true }}
        confirmLoading={submitting}
        width={460}
      >
        <p style={{ marginBottom: 8 }}>拒绝原因会通知给作者:</p>
        <Input.TextArea
          rows={3}
          placeholder="如：包含推广引流内容，请修改后重新提交"
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
        />
      </Modal>

      {/* 详情弹窗 */}
      <Modal
        title="动态内容"
        open={detailTarget != null}
        onCancel={() => setDetailTarget(null)}
        footer={null}
        width={520}
      >
        {detailTarget && (
          <div>
            <div style={{ marginBottom: 8, color: token.colorTextTertiary, fontSize: 13 }}>
              {detailTarget.userNickname} · {detailTarget.createdAt?.replace('T', ' ')}
            </div>
            <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontSize: 14, lineHeight: 1.7 }}>
              {detailTarget.content}
            </div>
            {detailTarget.images && detailTarget.images.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 12 }}>
                {detailTarget.images.map((url, i) => (
                  <img key={i} src={url} alt="" style={{ width: 90, height: 90, objectFit: 'cover', borderRadius: 8 }} />
                ))}
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
