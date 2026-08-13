import { useEffect, useState } from 'react';
import { Table, Input, Space, Tag, theme } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface AuditLogRecord {
  id: number; operatorId: number; operatorNickname: string;
  action: string; targetType: string; targetId: number;
  detail: string; result: string; createdAt: string;
}

const ACTION_TAG: Record<string, { label: string; color: string }> = {
  ban: { label: '封禁', color: 'red' },
  report_handle: { label: '举报处理', color: 'orange' },
  post_review: { label: '动态审核', color: 'blue' },
  post_delete: { label: '删除动态', color: 'red' },
  post_flag: { label: '置顶/精华', color: 'purple' },
  mate_status: { label: '搭子处置', color: 'geekblue' },
};

export default function AuditLogs() {
  const { token } = theme.useToken();
  const [data, setData] = useState<AuditLogRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [action, setAction] = useState('');
  const [keyword, setKeyword] = useState('');

  const fetchData = (p = 1) => {
    setLoading(true);
    request.get('/admin/audit-logs', {
      params: { page: p, size: 20, action: action || undefined, keyword: keyword || undefined },
    })
      .then((res) => {
        setData(res.data.data.records);
        setTotal(res.data.data.total);
        setPage(p);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1); }, []);

  const columns: ColumnsType<AuditLogRecord> = [
    { title: '序号', width: 70, render: (_, __, i) => (page - 1) * 20 + i + 1 },
    {
      title: '操作者', width: 140, render: (_, r) => (
        <span>{r.operatorNickname || `ID:${r.operatorId ?? '-'}`}</span>
      ),
    },
    {
      title: '动作', dataIndex: 'action', width: 110, render: (v: string) => {
        const t = ACTION_TAG[v];
        return t ? <Tag color={t.color}>{t.label}</Tag> : <Tag>{v}</Tag>;
      },
    },
    { title: '目标类型', dataIndex: 'targetType', width: 100 },
    { title: '目标ID', dataIndex: 'targetId', width: 90 },
    { title: '详情', dataIndex: 'detail', ellipsis: true },
    { title: '结果', dataIndex: 'result', width: 140, ellipsis: true },
    { title: '时间', dataIndex: 'createdAt', width: 170, render: (v: string) => v?.replace('T', ' ') },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>操作审计日志</h2>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="按详情关键字过滤"
          style={{ width: 220 }}
          allowClear
          onSearch={(v) => { setKeyword(v); fetchData(1); }}
        />
        <Input.Search
          placeholder="按动作过滤(如 post_review)"
          style={{ width: 220 }}
          allowClear
          onSearch={(v) => { setAction(v); fetchData(1); }}
        />
      </Space>
      <p style={{ color: token.colorTextTertiary, fontSize: 12, marginBottom: 8 }}>
        记录封禁、举报处理、内容审核、删除/下架等敏感操作,供复盘与合规追溯。
      </p>
      <Table
        columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{
          current: page, total, pageSize: 20,
          onChange: (p) => fetchData(p),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />
    </div>
  );
}
