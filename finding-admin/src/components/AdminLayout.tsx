import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Button, theme, Breadcrumb } from 'antd';
import {
  DashboardOutlined, UserOutlined, SafetyOutlined,
  FileTextOutlined, PictureOutlined, NotificationOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined, LogoutOutlined,
} from '@ant-design/icons';

const { Header, Sider, Content, Footer } = Layout;

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '数据面板' },
  { key: '/users', icon: <UserOutlined />, label: '用户管理' },
  { key: '/verification', icon: <SafetyOutlined />, label: '实名审核' },
  { key: '/posts', icon: <FileTextOutlined />, label: '动态管理' },
  { key: '/banners', icon: <PictureOutlined />, label: '轮播管理' },
  { key: '/announcements', icon: <NotificationOutlined />, label: '系统公告' },
];

const breadcrumbMap: Record<string, string> = {
  '/dashboard': '数据面板',
  '/users': '用户管理',
  '/verification': '实名审核',
  '/posts': '动态管理',
  '/banners': '轮播管理',
  '/announcements': '系统公告',
};

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();

  const pathSnippets = location.pathname.split('/').filter((i) => i);
  const breadcrumbItems = [
    { title: '首页', path: '/dashboard' },
    ...pathSnippets.map((_, index) => {
      const url = `/${pathSnippets.slice(0, index + 1).join('/')}`;
      return { title: breadcrumbMap[url] || url, path: url };
    }),
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        theme="light"
        width={220}
        style={{
          borderRight: `1px solid ${token.colorBorderSecondary}`,
          boxShadow: collapsed ? 'none' : '2px 0 8px rgba(0,0,0,0.04)',
        }}
      >
        {/* Logo area */}
        <div
          style={{
            height: 56,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 700,
            fontSize: collapsed ? 18 : 22,
            color: '#ff6b81',
            borderBottom: `1px solid ${token.colorBorderSecondary}`,
            letterSpacing: 2,
          }}
        >
          {collapsed ? 'F' : 'Finding'}
        </div>

        {/* Navigation menu */}
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ borderRight: 0, marginTop: 4 }}
        />
      </Sider>

      <Layout>
        {/* Top header bar */}
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: `1px solid ${token.colorBorderSecondary}`,
            height: 56,
            boxShadow: '0 1px 4px rgba(0,0,0,0.04)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              style={{ fontSize: 16 }}
            />
            <Breadcrumb
              items={breadcrumbItems.map((item) => ({
                title: (
                  <a
                    onClick={() => navigate(item.path)}
                    style={{ color: item.path === location.pathname ? token.colorText : token.colorTextSecondary }}
                  >
                    {item.title}
                  </a>
                ),
              }))}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span style={{ color: token.colorTextSecondary, fontSize: 13 }}>
              管理员
            </span>
            <Button
              type="text"
              icon={<LogoutOutlined />}
              onClick={() => navigate('/login')}
              danger
            >
              退出登录
            </Button>
          </div>
        </Header>

        {/* Main content area */}
        <Content
          style={{
            margin: 20,
            padding: 24,
            background: '#fff',
            borderRadius: 8,
            minHeight: 280,
            overflow: 'auto',
            boxShadow: '0 1px 4px rgba(0,0,0,0.04)',
          }}
        >
          <Outlet />
        </Content>

        {/* Footer */}
        <Footer style={{ textAlign: 'center', color: token.colorTextSecondary, fontSize: 13, padding: '12px 0' }}>
          Finding Admin ©2026 — 山东理工大学
        </Footer>
      </Layout>
    </Layout>
  );
}
