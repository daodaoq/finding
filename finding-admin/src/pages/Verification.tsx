import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Modal, Image, message, Input, Tabs } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import request from '../api/request';

interface VerifyRecord {
  id: number;
  userId: number;
  phone: string;
  realName: string;
  studentId: string;
  school: string;
  idCardFront?: string;
  idCardBack?: string;
  studentCard?: string;
  status: number;
  reviewComment?: string;
  createdAt: string;
}

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待审核', color: 'processing' },
  1: { label: '已通过', color: 'success' },
  2: { label: '已拒绝', color: 'error' },
};

const FALLBACK_IMG = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGRvbWluYW50LWJhc2VsaW5lPSJtaWRkbGUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZpbGw9IiNjY2MiIGZvbnQtc2l6ZT0iMTYiPuWbvueJh+WKoOi9veWksei0pTwvdGV4dD48L3N2Zz4=';

export default function Verification() {
  const [data, setData] = useState<VerifyRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [activeTab, setActiveTab] = useState('0');
  const [materialVisible, setMaterialVisible] = useState(false);
  const [materials, setMaterials] = useState<VerifyRecord | null>(null);
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [rejectComment, setRejectComment] = useState('');
  const [currentId, setCurrentId] = useState<number | null>(null);

  const fetchData = async (p = 1, status?: string) => {
    setLoading(true);
    try {
      const res = await request.get('/admin/verifications', {
        params: {
          page: p,
          size: 10,
          status: status === 'all' ? undefined : Number(status || activeTab),
        },
      });
      const body = res.data;
      if (body?.data) {
        setData(body.data.records ?? []);
        setTotal(body.data.total ?? 0);
        setPage(p);
      }
    } catch {
      message.error('获取认证列表失败，请确认已使用管理员账号登录');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData(1, activeTab);
  }, [activeTab]);

  const handleApprove = async (id: number) => {
    try {
      await request.put(`/admin/verifications/${id}/approve`);
      message.success('已通过');
      fetchData(page, activeTab);
    } catch {
      message.error('操作失败');
    }
  };

  const handleReject = async () => {
    if (currentId == null) return;
    try {
      await request.put(`/admin/verifications/${currentId}/reject`, null, {
        params: { comment: rejectComment },
      });
      message.success('已拒绝');
      setRejectModalVisible(false);
      setRejectComment('');
      setCurrentId(null);
      fetchData(page, activeTab);
    } catch {
      message.error('操作失败');
    }
  };

  const showMaterials = (record: VerifyRecord) => {
    setMaterials(record);
    setMaterialVisible(true);
  };

  const columns: ColumnsType<VerifyRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '手机号', dataIndex: 'phone', width: 130 },
    { title: '真实姓名', dataIndex: 'realName', width: 100 },
    { title: '学号', dataIndex: 'studentId', width: 120 },
    { title: '学校', dataIndex: 'school', width: 140 },
    {
      title: '状态', dataIndex: 'status', width: 100,
      render: (v: number) => <Tag color={STATUS_MAP[v]?.color}>{STATUS_MAP[v]?.label}</Tag>,
    },
    {
      title: '提交时间', dataIndex: 'createdAt', width: 160,
      render: (v: string) => v?.replace('T', ' '),
    },
    {
      title: '操作', width: 260,
      render: (_, record) => (
        <Space>
          {record.status === 0 && (
            <>
              <Button type="primary" size="small" onClick={() => handleApprove(record.id)}>
                通过
              </Button>
              <Button
                danger size="small"
                onClick={() => { setCurrentId(record.id); setRejectModalVisible(true); }}
              >
                拒绝
              </Button>
            </>
          )}
          <a onClick={() => showMaterials(record)}>查看材料</a>
        </Space>
      ),
    },
  ];

  const tabItems = [
    { key: '0', label: '待审核' },
    { key: '1', label: '已通过' },
    { key: '2', label: '已拒绝' },
    { key: 'all', label: '全部' },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>学生认证审核</h2>

      <Tabs
        activeKey={activeTab}
        onChange={(key) => setActiveTab(key)}
        items={tabItems}
      />

      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          total,
          pageSize: 10,
          onChange: (p) => fetchData(p, activeTab),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />

      {/* 查看材料弹窗 */}
      <Modal
        title="认证材料"
        open={materialVisible}
        onCancel={() => setMaterialVisible(false)}
        footer={null}
        width={600}
      >
        {materials && (
          <div>
            <p><b>姓名：</b>{materials.realName}</p>
            <p><b>学号：</b>{materials.studentId}</p>
            <p><b>学校：</b>{materials.school}</p>
            {materials.studentCard && (
              <div style={{ marginTop: 12 }}>
                <p><b>学生证：</b></p>
                <Image
                  src={materials.studentCard}
                  style={{ maxWidth: 400 }}
                  fallback={FALLBACK_IMG}
                />
              </div>
            )}
            {materials.reviewComment && (
              <p style={{ marginTop: 12, color: '#ff4d4f' }}>
                <b>拒绝原因：</b>{materials.reviewComment}
              </p>
            )}
          </div>
        )}
      </Modal>

      {/* 拒绝原因弹窗 */}
      <Modal
        title="拒绝认证"
        open={rejectModalVisible}
        onOk={handleReject}
        onCancel={() => { setRejectModalVisible(false); setRejectComment(''); setCurrentId(null); }}
        okText="确认拒绝"
        okButtonProps={{ danger: true }}
      >
        <p>请输入拒绝原因（选填）：</p>
        <Input.TextArea
          rows={3}
          placeholder="如：学号与姓名不匹配"
          value={rejectComment}
          onChange={(e) => setRejectComment(e.target.value)}
        />
      </Modal>
    </div>
  );
}
