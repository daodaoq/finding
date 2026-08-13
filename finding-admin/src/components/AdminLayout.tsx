import { useEffect, useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Button, theme, Breadcrumb, type MenuProps } from 'antd';
import { logoutAdmin } from '../utils/adminAuth';
import {
  DashboardOutlined, UserOutlined, FileTextOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined, LogoutOutlined, TeamOutlined,
} from '@ant-design/icons';

const { Header, Sider, Content, Footer } = Layout;

const menuItems: MenuProps['items'] = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '数据概览' },
  {
    key: 'users-and-risk',
    icon: <UserOutlined />,
    label: '用户与风控',
    children: [
      { key: '/users', label: '用户管理' },
      { key: '/verification', label: '实名审核' },
      { key: '/reports', label: '投诉管理' },
      { key: '/appeals', label: '申诉管理' },
      { key: '/feedback', label: '用户反馈' },
      { key: '/audit-logs', label: '操作审计' },
    ],
  },
  {
    key: 'content-operations',
    icon: <FileTextOutlined />,
    label: '内容运营',
    children: [
      { key: '/posts', label: '动态管理' },
      { key: '/post-review', label: '动态审核' },
      { key: '/love-guide-review', label: '恋爱干货审核' },
      { key: '/comments', label: '评论管理' },
      { key: '/banners', label: '轮播管理' },
      { key: '/announcements', label: '系统公告' },
      { key: '/banned-words', label: '违禁词管理' },
      { key: '/image-moderation', label: '图片审核' },
    ],
  },
  {
    key: 'social-and-activities',
    icon: <TeamOutlined />,
    label: '社交与活动',
    children: [
      { key: '/mates', label: '搭子管理' },
      { key: '/mate-review', label: '搭子审核' },
      { key: '/groups', label: '群聊管理' },
      { key: '/chat-audit', label: '聊天审查' },
    ],
  },
];

const groupByPath: Record<string, string> = {
  '/users': 'users-and-risk',
  '/verification': 'users-and-risk',
  '/reports': 'users-and-risk',
  '/appeals': 'users-and-risk',
  '/feedback': 'users-and-risk',
  '/audit-logs': 'users-and-risk',
  '/posts': 'content-operations',
  '/post-review': 'content-operations',
  '/love-guide-review': 'content-operations',
  '/comments': 'content-operations',
  '/banners': 'content-operations',
  '/announcements': 'content-operations',
  '/banned-words': 'content-operations',
  '/image-moderation': 'content-operations',
  '/mates': 'social-and-activities',
  '/mate-review': 'social-and-activities',
  '/groups': 'social-and-activities',
  '/chat-audit': 'social-and-activities',
};

const breadcrumbMap: Record<string, string> = {
  '/dashboard': '数据面板',
  '/users': '用户管理',
  '/verification': '实名审核',
  '/reports': '投诉管理',
  '/posts': '动态管理',
  '/post-review': '动态审核',
  '/love-guide-review': '恋爱干货审核',
  '/mate-review': '搭子审核',
  '/appeals': '申诉管理',
  '/comments': '评论管理',
  '/mates': '搭子管理',
  '/groups': '群聊管理',
  '/banners': '轮播管理',
  '/announcements': '系统公告',
  '/banned-words': '违禁词管理',
  '/image-moderation': '图片审核',
  '/chat-audit': '聊天审查',
  '/feedback': '用户反馈',
  '/audit-logs': '操作审计',
};

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();
  const activeGroup = groupByPath[location.pathname];
  const [openKeys, setOpenKeys] = useState<string[]>(activeGroup ? [activeGroup] : []);

  useEffect(() => {
    if (activeGroup) setOpenKeys([activeGroup]);
  }, [activeGroup]);

  // 登出:集中清理 token 并回登录页(路由级守卫 RequireAdminAuth 负责拦截未登录访问)
  const handleLogout = () => {
    logoutAdmin('已退出登录');
  };

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
        <div
          style={{
            height: 56,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 700,
            fontSize: collapsed ? 18 : 22,
            color: token.colorPrimary,
            borderBottom: `1px solid ${token.colorBorderSecondary}`,
            letterSpacing: 2,
          }}
        >
          {collapsed ? 'F' : 'Finding'}
        </div>

        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          openKeys={openKeys}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          onOpenChange={(keys) => setOpenKeys(keys as string[])}
          style={{ borderRight: 0, marginTop: 4 }}
        />
      </Sider>

      <Layout>
        <Header
          style={{
            background: token.colorBgContainer,
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
              onClick={handleLogout}
              danger
            >
              退出登录
            </Button>
          </div>
        </Header>

        <Content
          style={{
            margin: 20,
            padding: 24,
            background: token.colorBgContainer,
            borderRadius: 8,
            minHeight: 280,
            overflow: 'auto',
            boxShadow: '0 1px 4px rgba(0,0,0,0.04)',
          }}
        >
          <Outlet />
        </Content>

        <Footer style={{ textAlign: 'center', color: token.colorTextSecondary, fontSize: 13, padding: '12px 0' }}>
          Finding Admin ©2026 — 山东理工大学
        </Footer>
      </Layout>
    </Layout>
  );
}
