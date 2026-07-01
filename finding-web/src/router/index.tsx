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
import ChatDetailPage from '../pages/Chat';
import LoginPage from '../pages/Login';
import RegisterPage from '../pages/Register';

const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'square', element: <SquarePage /> },
      { path: 'mate', element: <MatePage /> },
      { path: 'messages', element: <MessagesPage /> },
      { path: 'messages/notifications', element: <NotificationsPage /> },
      { path: 'messages/chat', element: <ChatDetailPage /> },
      { path: 'mine', element: <MinePage /> },
      { path: 'mine/posts', element: <MyPostsPage /> },
      { path: 'mine/likes', element: <MyLikesPage /> },
      { path: 'mine/mates', element: <MyMatesPage /> },
      { path: 'mine/invitations', element: <MyInvitationsPage /> },
      { path: 'mine/joined', element: <MyJoinedPage /> },
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

export default router;
