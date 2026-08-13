import { useEffect, useState } from 'react';
import { Card, Col, Row, Statistic, theme } from 'antd';
import {
  UserOutlined, FileTextOutlined, TeamOutlined, CheckCircleOutlined,
  UserAddOutlined, WarningOutlined, UsergroupAddOutlined,
} from '@ant-design/icons';
import request from '../api/request';

interface DashboardStats {
  totalUsers: number;
  todayPosts: number;
  todayMates: number;
  pendingVerifications: number;
  todayNewUsers: number;
  totalMates: number;
  pendingReports: number;
  groupCount: number;
}

interface TrendData {
  dates: string[];
  newUsers: number[];
  newPosts: number[];
  newMates: number[];
  activeUsers: number[];
}

interface QualityData {
  genderRatio: { male: number; female: number; maleRate: number; femaleRate: number };
  verificationRate: { approved: number; total: number; rate: number };
  retention: { active7d: number; activeRate: number; retained7d: number; retentionRate: number };
  moderationSla: {
    pendingPosts: number; pendingAppeals: number; pendingVerifications: number; pendingReports: number;
    oldestPendingPostHours: number; oldestPendingAppealHours: number; oldestPendingVerificationHours: number;
  };
}

export default function Dashboard() {
  const { token } = theme.useToken();
  const [stats, setStats] = useState<DashboardStats>({
    totalUsers: 0, todayPosts: 0, todayMates: 0, pendingVerifications: 0,
    todayNewUsers: 0, totalMates: 0, pendingReports: 0, groupCount: 0,
  });
  const [trend, setTrend] = useState<TrendData | null>(null);
  const [quality, setQuality] = useState<QualityData | null>(null);

  useEffect(() => {
    request.get('/admin/dashboard').then((res) => {
      if (res.data?.data) setStats(res.data.data);
    }).catch(() => {});
    request.get('/admin/dashboard/trend', { params: { days: 7 } }).then((res) => {
      if (res.data?.data) setTrend(res.data.data);
    }).catch(() => {});
    request.get('/admin/dashboard/quality').then((res) => {
      if (res.data?.data) setQuality(res.data.data);
    }).catch(() => {});
  }, []);

  const renderBars = (values: number[], color: string) => {
    const max = Math.max(1, ...(values || []));
    return (
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8, height: 120 }}>
        {values.map((v, i) => (
          <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
            <span style={{ fontSize: 11, color: token.colorTextSecondary }}>{v}</span>
            <div
              style={{
                width: '100%', background: color, borderRadius: 3,
                height: `${Math.round((v / max) * 90)}%`, minHeight: v > 0 ? 4 : 1,
                opacity: v > 0 ? 1 : 0.25,
              }}
            />
            <span style={{ fontSize: 10, color: token.colorTextTertiary }}>{trend?.dates?.[i]?.slice(5)}</span>
          </div>
        ))}
      </div>
    );
  };

  const trendCard = (title: string, values: number[], color: string) => (
    <Card title={title} style={{ marginTop: 16 }}>
      {renderBars(values || [], color)}
    </Card>
  );

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>数据面板</h2>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="总用户数" value={stats.totalUsers} prefix={<UserOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="今日动态" value={stats.todayPosts} prefix={<FileTextOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="搭子邀约" value={stats.todayMates} prefix={<TeamOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="待审核认证" value={stats.pendingVerifications} prefix={<CheckCircleOutlined />} valueStyle={{ color: token.colorPrimary }} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="今日新增用户" value={stats.todayNewUsers} prefix={<UserAddOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="搭子总数" value={stats.totalMates} prefix={<TeamOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="待处理投诉" value={stats.pendingReports} prefix={<WarningOutlined />} valueStyle={{ color: token.colorError }} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="群聊总数" value={stats.groupCount} prefix={<UsergroupAddOutlined />} /></Card>
        </Col>
      </Row>
      <Card title="近 7 天趋势" style={{ marginTop: 16 }}>
        {trend ? (
          <div>
            {trendCard('新增用户', trend.newUsers, token.colorPrimary)}
            {trendCard('新增动态', trend.newPosts, token.colorSuccess)}
            {trendCard('新增搭子', trend.newMates, token.colorInfo)}
            {trendCard('活跃用户(登录)', trend.activeUsers, token.colorWarning)}
          </div>
        ) : (
          <p style={{ color: token.colorTextTertiary, textAlign: 'center', padding: 20 }}>加载中...</p>
        )}
      </Card>
      <Card title="质量与漏斗指标" style={{ marginTop: 16 }}>
        {quality ? (
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} lg={6}>
              <Card size="small" title="性别比">
                <p>男 {quality.genderRatio.male}（{quality.genderRatio.maleRate}%）</p>
                <p>女 {quality.genderRatio.female}（{quality.genderRatio.femaleRate}%）</p>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card size="small" title="认证率">
                <Statistic value={quality.verificationRate.rate} suffix="%" precision={2} />
                <p style={{ color: token.colorTextSecondary }}>已认证 {quality.verificationRate.approved} / {quality.verificationRate.total}</p>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card size="small" title="留存">
                <p>7 日活跃 {quality.retention.active7d}（{quality.retention.activeRate}%）</p>
                <p>老用户留存率 {quality.retention.retentionRate}%</p>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card size="small" title="审核时效">
                <p>待审:动态 {quality.moderationSla.pendingPosts} / 申诉 {quality.moderationSla.pendingAppeals}</p>
                <p>认证 {quality.moderationSla.pendingVerifications} / 投诉 {quality.moderationSla.pendingReports}</p>
                <p style={{ color: token.colorWarning }}>最久待审动态 {quality.moderationSla.oldestPendingPostHours}h</p>
              </Card>
            </Col>
          </Row>
        ) : (
          <p style={{ color: token.colorTextTertiary, textAlign: 'center', padding: 20 }}>加载中...</p>
        )}
      </Card>
      <Card title="快速入口" style={{ marginTop: 16 }}>
        <p>📋 <a href="/verification">实名认证审核</a> — 待审核: {stats.pendingVerifications}条</p>
        <p>📝 <a href="/posts">动态内容管理</a> — 今日新增: {stats.todayPosts}条</p>
        <p>⚠️ <a href="/reports">投诉管理</a> — 待处理: {stats.pendingReports}条</p>
        <p>🖼️ <a href="/banners">首页轮播管理</a></p>
      </Card>
    </div>
  );
}
