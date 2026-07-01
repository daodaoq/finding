import { createBrowserRouter, Navigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import AuthLayout from '../layouts/AuthLayout';
import HomePage from '../pages/Home';
import SquarePage from '../pages/Square';
import MatePage from '../pages/Mate';
import MessagesPage from '../pages/Messages';
import NotificationsPage from '../pages/Notifications';
import MinePage from '../pages/Mine';
import MyPostsPage from '../pages/Mine/MyPosts';
import MyLikesPage from '../pages/Mine/MyLikes';
import MyMatesPage from '../pages/Mine/MyMates';
import MyInvitationsPage from '../pages/Mine/MyInvitations';
import MyJoinedPage from '../pages/Mine/MyJoined';
import PostDetailPage from '../pages/PostDetail';
import MateDetailPage from '../pages/MateDetail';
import ChatDetailPage from '../pages/Chat';
import CreatePostPage from '../pages/CreatePost';
import CreateMatePage from '../pages/CreateMate';
import LoginPage from '../pages/Login';
import RegisterPage from '../pages/Register';

const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'square', element: <SquarePage /> },
      { path: 'square/post/:id', element: <PostDetailPage /> },
      { path: 'mate', element: <MatePage /> },
      { path: 'mate/:id', element: <MateDetailPage /> },
      { path: 'messages', element: <MessagesPage /> },
      { path: 'messages/notifications', element: <NotificationsPage /> },
      { path: 'messages/chat', element: <ChatDetailPage /> },
      { path: 'mine', element: <MinePage /> },
      { path: 'mine/posts', element: <MyPostsPage /> },
      { path: 'mine/likes', element: <MyLikesPage /> },
      { path: 'mine/mates', element: <MyMatesPage /> },
      { path: 'mine/invitations', element: <MyInvitationsPage /> },
      { path: 'mine/joined', element: <MyJoinedPage /> },
      { path: 'create-post', element: <CreatePostPage /> },
      { path: 'create-mate', element: <CreateMatePage /> },
    ],
  },
  {
    element: <AuthLayout />,
    children: [
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);

/** 消除 React Router v7 future flag warning（当前版本不支持 object form future flags） */
/* 升级 react-router-dom 到 v7 后移除上述注释并使用: future: { v7_startTransition: true } */

export default router;
