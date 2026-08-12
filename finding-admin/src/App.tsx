import { BrowserRouter, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { registerNavigator, unregisterNavigator } from './utils/adminAuth';
import AdminLayout from './components/AdminLayout';
import RequireAdminAuth from './components/RequireAdminAuth';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import Verification from './pages/Verification';
import Reports from './pages/Reports';
import Posts from './pages/Posts';
import PostReview from './pages/PostReview';
import MateReview from './pages/MateReview';
import Appeals from './pages/Appeals';
import Comments from './pages/Comments';
import Mates from './pages/Mates';
import Groups from './pages/Groups';
import Banners from './pages/Banners';
import Announcements from './pages/Announcements';
import ForbiddenWords from './pages/ForbiddenWords';
import ChatAudit from './pages/ChatAudit';
import Feedback from './pages/Feedback';
import Login from './pages/Login';

/**
 * 将 navigate 注册到模块级导航器,供 axios 拦截器等 Router 上下文外的代码 SPA 内跳转。
 * 始终挂载(无论登录态),避免 401 时整页刷新。
 */
function RouterNavigator() {
  const navigate = useNavigate();
  useEffect(() => {
    registerNavigator(navigate);
    return () => unregisterNavigator();
  }, [navigate]);
  return null;
}

/**
 * 管理端主题 token —— 与用户端设计令牌解耦,独立维护。
 * 后台以信息密度与状态色清晰为优先:语义色(成功/警告/错误/信息)显式声明,
 * 组件一律通过 theme.useToken() 消费,不再复制粘贴色值。
 */
const adminTheme = {
  token: {
    colorPrimary: '#ff6b81', // 品牌色(管理端独有,改动仅影响后台)
    colorSuccess: '#52c41a', // 状态绿:通过/正常
    colorWarning: '#faad14', // 状态橙:待处理/警告
    colorError: '#ff4d4f',   // 状态红:拒绝/封禁
    colorInfo: '#1677ff',    // 信息蓝:链接/提示
    borderRadius: 6,         // 小圆角,提升信息密度
  },
  components: {
    Table: { headerBg: '#fafafa' },
  },
};

export default function App() {
  return (
    <ConfigProvider locale={zhCN} theme={adminTheme}>
      <BrowserRouter basename="/admin">
        <RouterNavigator />
        <Routes>
          <Route path="/login" element={<Login />} />
          {/* 受保护路由:未登录一律跳登录页 */}
          <Route element={<RequireAdminAuth />}>
            <Route path="/" element={<AdminLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="users" element={<Users />} />
            <Route path="verification" element={<Verification />} />
            <Route path="reports" element={<Reports />} />
            <Route path="posts" element={<Posts />} />
            <Route path="post-review" element={<PostReview />} />
            <Route path="mate-review" element={<MateReview />} />
            <Route path="appeals" element={<Appeals />} />
            <Route path="comments" element={<Comments />} />
            <Route path="mates" element={<Mates />} />
            <Route path="groups" element={<Groups />} />
            <Route path="banners" element={<Banners />} />
            <Route path="announcements" element={<Announcements />} />
            <Route path="banned-words" element={<ForbiddenWords />} />
            <Route path="chat-audit" element={<ChatAudit />} />
            <Route path="feedback" element={<Feedback />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
}
