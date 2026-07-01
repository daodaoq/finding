import { Card, Col, Row, Statistic } from 'antd';
import { UserOutlined, FileTextOutlined, TeamOutlined, CheckCircleOutlined } from '@ant-design/icons';

export default function Dashboard() {
  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>数据面板</h2>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="总用户数" value={12850} prefix={<UserOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="今日动态" value={342} prefix={<FileTextOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="搭子邀约" value={156} prefix={<TeamOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="待审核认证" value={28} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#ff6b81' }} /></Card>
        </Col>
      </Row>
      <Card title="快速入口" style={{ marginTop: 16 }}>
        <p>📋 <a href="/verification">实名认证审核</a> — 待审核: 28条</p>
        <p>📝 <a href="/posts">动态内容管理</a> — 今日新增: 342条</p>
        <p>🖼️ <a href="/banners">首页轮播管理</a> — 当前有效: 3条</p>
      </Card>
    </div>
  );
}
