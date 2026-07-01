import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import AdminLayout from './components/AdminLayout';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import Verification from './pages/Verification';
import Posts from './pages/Posts';
import Banners from './pages/Banners';
import Announcements from './pages/Announcements';
import Login from './pages/Login';

export default function App() {
  return (
    <ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: '#ff6b81' } }}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<AdminLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="users" element={<Users />} />
            <Route path="verification" element={<Verification />} />
            <Route path="posts" element={<Posts />} />
            <Route path="banners" element={<Banners />} />
            <Route path="announcements" element={<Announcements />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
}
