import { useEffect, useState } from 'react';
import { Card, Col, Row, Statistic } from 'antd';
import { UserOutlined, FileTextOutlined, TeamOutlined, CheckCircleOutlined } from '@ant-design/icons';
import request from '../api/request';

interface DashboardStats {
  totalUsers: number;
  todayPosts: number;
  todayMates: number;
  pendingVerifications: number;
}

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats>({
    totalUsers: 0, todayPosts: 0, todayMates: 0, pendingVerifications: 0,
  });

  useEffect(() => {
    request.get('/admin/dashboard').then((res) => {
      if (res.data?.data) setStats(res.data.data);
    }).catch(() => {});
  }, []);

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
          <Card><Statistic title="待审核认证" value={stats.pendingVerifications} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#ff6b81' }} /></Card>
        </Col>
      </Row>
      <Card title="快速入口" style={{ marginTop: 16 }}>
        <p>📋 <a href="/verification">实名认证审核</a> — 待审核: {stats.pendingVerifications}条</p>
        <p>📝 <a href="/posts">动态内容管理</a> — 今日新增: {stats.todayPosts}条</p>
        <p>🖼️ <a href="/banners">首页轮播管理</a></p>
      </Card>
    </div>
  );
}
