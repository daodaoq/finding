import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Popconfirm, Modal, Input, message, Switch, Radio, theme, Upload, Alert } from 'antd';
import { UploadOutlined, DownloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadRequestOption } from 'rc-upload/lib/interface';
import request from '../api/request';
import { downloadForbiddenWordTemplate } from '../utils/download';

interface ForbiddenWordRecord {
  id: number; word: string; status: number; action: number; createdAt: string;
}

/** 批量导入结果(后端返回) */
interface ImportResult {
  total: number;
  imported: number;
  skipped: number;
  failed: { row: number; word: string; reason: string }[];
}

export default function ForbiddenWords() {
  const { token } = theme.useToken();
  const [data, setData] = useState<ForbiddenWordRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<ForbiddenWordRecord | null>(null);
  const [form, setForm] = useState({ word: '', status: 1, action: 0 });
  // Excel 批量导入
  const [importOpen, setImportOpen] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importAction, setImportAction] = useState(0);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);

  const fetchData = (p = 1, kw?: string) => {
    setLoading(true);
    request.get('/admin/forbidden-words', { params: { page: p, size: 10, keyword: kw || keyword } })
      .then((res) => {
        setData(res.data.data.records);
        setTotal(res.data.data.total);
        setPage(p);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(1); }, []);

  const openCreate = () => {
    setEditing(null);
    setForm({ word: '', status: 1, action: 0 });
    setModalOpen(true);
  };

  const openEdit = (record: ForbiddenWordRecord) => {
    setEditing(record);
    setForm({ word: record.word, status: record.status, action: record.action ?? 0 });
    setModalOpen(true);
  };

  const handleSave = async () => {
    if (!form.word.trim()) { message.warning('请输入违禁词'); return; }
    try {
      if (editing) {
        await request.put(`/admin/forbidden-words/${editing.id}`, { word: form.word.trim(), action: form.action });
        message.success('已更新');
      } else {
        await request.post('/admin/forbidden-words', { word: form.word.trim(), action: form.action });
        message.success('已新增');
      }
      setModalOpen(false);
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  // ── Excel 批量导入 ──
  const openImport = () => {
    setImportAction(0);
    setImportResult(null);
    setImportOpen(true);
  };

  const handleImport = async (options: UploadRequestOption) => {
    setImporting(true);
    setImportResult(null);
    try {
      const fd = new FormData();
      fd.append('file', options.file as File);
      const res = await request.post(`/admin/forbidden-words/import?action=${importAction}`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      const result: ImportResult = res.data?.data;
      setImportResult(result);
      options.onSuccess?.(result);
      if (result.imported > 0) {
        message.success(`成功导入 ${result.imported} 条违禁词`);
        fetchData(1);
      } else {
        message.warning('没有新增词条，请查看下方结果');
      }
    } catch (e: any) {
      message.error(e?.message || '导入失败，请检查文件格式');
      options.onError?.(e instanceof Error ? e : new Error('导入失败'));
    } finally {
      setImporting(false);
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      await downloadForbiddenWordTemplate();
    } catch {
      message.error('模板下载失败');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/admin/forbidden-words/${id}`);
      message.success('已删除');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const toggleStatus = async (record: ForbiddenWordRecord) => {
    const newStatus = record.status === 1 ? 0 : 1;
    try {
      await request.put(`/admin/forbidden-words/${record.id}/status`, { status: newStatus });
      message.success(newStatus === 1 ? '已启用' : '已禁用');
      fetchData(page);
    } catch { message.error('操作失败'); }
  };

  const columns: ColumnsType<ForbiddenWordRecord> = [
    { title: '序号', width: 60, render: (_, __, i) => (page - 1) * 10 + i + 1 },
    { title: '违禁词', dataIndex: 'word' },
    { title: '动作', dataIndex: 'action', width: 80, render: (v: number) => (
      <Tag color={v === 1 ? 'warning' : 'error'}>{v === 1 ? '送审' : '拦截'}</Tag>
    )},
    { title: '状态', dataIndex: 'status', render: (v: number) => (
      <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag>
    )},
    { title: '创建时间', dataIndex: 'createdAt', render: (v: string) => v?.replace('T', ' ') },
    {
      title: '操作', render: (_, record) => (
        <Space>
          <a onClick={() => openEdit(record)}>编辑</a>
          <Popconfirm title={`确定${record.status === 1 ? '禁用' : '启用'}该词？`} onConfirm={() => toggleStatus(record)}>
            <a style={{ color: record.status === 1 ? 'orange' : 'green' }}>
              {record.status === 1 ? '禁用' : '启用'}
            </a>
          </Popconfirm>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <a style={{ color: 'red' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>违禁词管理</h2>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          placeholder="搜索违禁词"
          style={{ width: 300 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={(v) => fetchData(1, v)}
          allowClear
        />
        <Button type="primary" onClick={openCreate}>+ 新增违禁词</Button>
        <Button icon={<UploadOutlined />} onClick={openImport}>Excel 批量导入</Button>
      </Space>
      <Table
        columns={columns} dataSource={data} rowKey="id" loading={loading}
        pagination={{
          current: page, total, pageSize: 10,
          onChange: (p) => fetchData(p),
          showTotal: (t) => `共 ${t} 条`,
        }}
      />

      <Modal
        title={editing ? '编辑违禁词' : '新增违禁词'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        okText={editing ? '保存' : '新增'}
        width={460}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Input
            placeholder="违禁词(中英文均可,保存后全站即时生效)"
            value={form.word}
            onChange={(e) => setForm({ ...form, word: e.target.value })}
            maxLength={100}
          />
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span>启用:</span>
            <Switch checked={form.status === 1} onChange={(v) => setForm({ ...form, status: v ? 1 : 0 })} />
            <span style={{ fontSize: 12, color: token.colorTextPlaceholder, marginLeft: 8 }}>
              禁用的词不会拦截用户内容
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span>动作:</span>
            <Radio.Group
              value={form.action}
              onChange={(e) => setForm({ ...form, action: e.target.value })}
              options={[
                { label: '拦截(命中即拒绝发布)', value: 0 },
                { label: '送审(命中进入审核队列)', value: 1 },
              ]}
            />
          </div>
        </div>
      </Modal>

      {/* Excel 批量导入 */}
      <Modal
        title="Excel 批量导入违禁词"
        open={importOpen}
        onCancel={() => setImportOpen(false)}
        footer={<Button onClick={() => setImportOpen(false)}>关闭</Button>}
        width={560}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* 格式示范 */}
          <div>
            <div style={{ marginBottom: 8 }}>
              表格<b>仅需一列:违禁词</b>,首行表头会自动跳过,空行忽略;导入的词默认启用。
            </div>
            <div style={{ border: `1px solid ${token.colorBorderSecondary}`, borderRadius: 6, overflow: 'hidden' }}>
              <div style={{ display: 'flex', background: token.colorFillTertiary, fontWeight: 600 }}>
                <div style={{ flex: 1, padding: '6px 12px' }}>违禁词</div>
              </div>
              {['示例词一', '示例词二'].map((w) => (
                <div key={w} style={{ display: 'flex', borderTop: `1px solid ${token.colorBorderSecondary}`, color: token.colorTextSecondary }}>
                  <div style={{ flex: 1, padding: '6px 12px' }}>{w}</div>
                </div>
              ))}
            </div>
            <Button type="link" size="small" icon={<DownloadOutlined />} style={{ paddingLeft: 0, marginTop: 4 }} onClick={handleDownloadTemplate}>
              下载 Excel 模板
            </Button>
          </div>

          {/* 动作选择 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span>动作:</span>
            <Radio.Group
              value={importAction}
              onChange={(e) => setImportAction(e.target.value)}
              options={[
                { label: '拦截', value: 0 },
                { label: '送审', value: 1 },
              ]}
            />
            <span style={{ fontSize: 12, color: token.colorTextPlaceholder }}>作用于本批全部词条</span>
          </div>

          {/* 上传 */}
          <Upload
            accept=".xlsx,.xls,.csv"
            showUploadList={false}
            customRequest={handleImport}
            disabled={importing}
          >
            <Button type="primary" icon={<UploadOutlined />} loading={importing}>
              {importing ? '导入中...' : '选择文件并导入'}
            </Button>
          </Upload>

          {/* 导入结果 */}
          {importResult && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <Alert
                type={importResult.imported > 0 ? 'success' : 'warning'}
                showIcon
                message={
                  `解析 ${importResult.total} 条：成功导入 ${importResult.imported} 条，` +
                  `跳过重复 ${importResult.skipped} 条，失败 ${importResult.failed.length} 条`
                }
              />
              {importResult.failed.length > 0 && (
                <Table
                  size="small"
                  rowKey={(r) => `${r.row}-${r.word}`}
                  dataSource={importResult.failed.slice(0, 20)}
                  pagination={false}
                  columns={[
                    { title: '行号', dataIndex: 'row', width: 70 },
                    { title: '违禁词', dataIndex: 'word', ellipsis: true },
                    { title: '原因', dataIndex: 'reason', width: 140 },
                  ]}
                />
              )}
              {importResult.failed.length > 20 && (
                <span style={{ fontSize: 12, color: token.colorTextPlaceholder }}>
                  仅展示前 20 条失败明细
                </span>
              )}
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}
