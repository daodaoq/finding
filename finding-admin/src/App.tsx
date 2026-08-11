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

export default function App() {
  return (
    <ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: '#ff6b81' } }}>
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
