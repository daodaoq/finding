import { lazy, Suspense, type ReactNode } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import AuthLayout from '../layouts/AuthLayout';

/* ── 页面懒加载：首屏只加载布局骨架，进入路由时才拉取对应页面 chunk ── */
const HomePage = lazy(() => import('../pages/Home'));
const BridgePage = lazy(() => import('../pages/Bridge'));
const SendApplyList = lazy(() => import('../pages/Bridge/SendApplyList'));
const ReceiveApplyList = lazy(() => import('../pages/Bridge/ReceiveApplyList'));
const MyCardPage = lazy(() => import('../pages/Bridge/MyCard'));
const SquarePage = lazy(() => import('../pages/Square'));
const MatePage = lazy(() => import('../pages/Mate'));
const MessagesPage = lazy(() => import('../pages/Messages'));
const NotificationsPage = lazy(() => import('../pages/Notifications'));
const MinePage = lazy(() => import('../pages/Mine'));
const MyPostsPage = lazy(() => import('../pages/Mine/MyPosts'));
const MyLikesPage = lazy(() => import('../pages/Mine/MyLikes'));
const MyMatesPage = lazy(() => import('../pages/Mine/MyMates'));
const MyInvitationsPage = lazy(() => import('../pages/Mine/MyInvitations'));
const MyJoinedPage = lazy(() => import('../pages/Mine/MyJoined'));
const MyApplicationsPage = lazy(() => import('../pages/Mine/MyApplications'));
const ProfileEditPage = lazy(() => import('../pages/Mine/ProfileEdit'));
const VerifyPage = lazy(() => import('../pages/Mine/Verify'));
const ResumeEditPage = lazy(() => import('../pages/Resume'));
const HistoryPage = lazy(() => import('../pages/Mine/History'));
const MyReportsPage = lazy(() => import('../pages/Mine/MyReports'));
const OrdersPage = lazy(() => import('../pages/Mine/Orders'));
const HelpPage = lazy(() => import('../pages/Mine/Help'));
const FAQPage = lazy(() => import('../pages/Mine/Help/FAQ'));
const ContactPage = lazy(() => import('../pages/Mine/Help/Contact'));
const GuidePage = lazy(() => import('../pages/Mine/Help/Guide'));
const HumanPage = lazy(() => import('../pages/Mine/Help/Human'));
const SettingsPage = lazy(() => import('../pages/Mine/Settings'));
const AccountPage = lazy(() => import('../pages/Mine/Account'));
const ChatSettingsGlobal = lazy(() => import('../pages/Mine/Settings/Chat'));
const FriendSetting = lazy(() => import('../pages/Mine/Settings/Friend'));
const PrivacySetting = lazy(() => import('../pages/Mine/Settings/Privacy'));
const PreferenceSetting = lazy(() => import('../pages/Mine/Settings/Preference'));
const AboutPage = lazy(() => import('../pages/Mine/About'));
const PostDetailPage = lazy(() => import('../pages/PostDetail'));
const MateDetailPage = lazy(() => import('../pages/MateDetail'));
const ChatDetailPage = lazy(() => import('../pages/Chat'));
const ChatSettingsPage = lazy(() => import('../pages/ChatSettings'));
const GroupChatPage = lazy(() => import('../pages/GroupChat'));
const GroupInfoPage = lazy(() => import('../pages/GroupInfo'));
const CreateGroupPage = lazy(() => import('../pages/CreateGroup'));
const UserProfilePage = lazy(() => import('../pages/UserProfile'));
const SearchPage = lazy(() => import('../pages/Search'));
const CreatePostPage = lazy(() => import('../pages/CreatePost'));
const CreateMatePage = lazy(() => import('../pages/CreateMate'));
const LoginPage = lazy(() => import('../pages/Login'));
const RegisterPage = lazy(() => import('../pages/Register'));

/** 路由切换时的懒加载占位 */
function PageFallback() {
  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      color: '#ccc',
      fontSize: 14,
    }}>
      加载中...
    </div>
  );
}

const withSuspense = (node: ReactNode) => (
  <Suspense fallback={<PageFallback />}>{node}</Suspense>
);

const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: withSuspense(<HomePage />) },
      { path: 'bridge', element: withSuspense(<BridgePage />) },
      { path: 'bridge/send-apply', element: withSuspense(<SendApplyList />) },
      { path: 'bridge/receive-apply', element: withSuspense(<ReceiveApplyList />) },
      { path: 'bridge/my-card', element: withSuspense(<MyCardPage />) },
      { path: 'square', element: withSuspense(<SquarePage />) },
      { path: 'square/post/:id', element: withSuspense(<PostDetailPage />) },
      { path: 'mate', element: withSuspense(<MatePage />) },
      { path: 'mate/:id', element: withSuspense(<MateDetailPage />) },
      { path: 'messages', element: withSuspense(<MessagesPage />) },
      { path: 'messages/notifications', element: withSuspense(<NotificationsPage />) },
      { path: 'messages/chat', element: withSuspense(<ChatDetailPage />) },
      { path: 'messages/chat-settings', element: withSuspense(<ChatSettingsPage />) },
      { path: 'messages/group-chat/:id', element: withSuspense(<GroupChatPage />) },
      { path: 'messages/group-chat/:id/info', element: withSuspense(<GroupInfoPage />) },
      { path: 'messages/create-group', element: withSuspense(<CreateGroupPage />) },
      { path: 'user/:id', element: withSuspense(<UserProfilePage />) },
      { path: 'search', element: withSuspense(<SearchPage />) },
      { path: 'mine', element: withSuspense(<MinePage />) },
      { path: 'mine/posts', element: withSuspense(<MyPostsPage />) },
      { path: 'mine/likes', element: withSuspense(<MyLikesPage />) },
      { path: 'mine/mates', element: withSuspense(<MyMatesPage />) },
      { path: 'mine/invitations', element: withSuspense(<MyInvitationsPage />) },
      { path: 'mine/joined', element: withSuspense(<MyJoinedPage />) },
      { path: 'mine/applications', element: withSuspense(<MyApplicationsPage />) },
      { path: 'mine/profile', element: withSuspense(<ProfileEditPage />) },
      { path: 'mine/verify', element: withSuspense(<VerifyPage />) },
      { path: 'mine/resume', element: withSuspense(<ResumeEditPage />) },
      { path: 'mine/history', element: withSuspense(<HistoryPage />) },
      { path: 'mine/reports', element: withSuspense(<MyReportsPage />) },
      { path: 'mine/orders', element: withSuspense(<OrdersPage />) },
      { path: 'mine/help', element: withSuspense(<HelpPage />) },
      { path: 'mine/help/faq', element: withSuspense(<FAQPage />) },
      { path: 'mine/help/contact', element: withSuspense(<ContactPage />) },
      { path: 'mine/help/guide', element: withSuspense(<GuidePage />) },
      { path: 'mine/help/human', element: withSuspense(<HumanPage />) },
      { path: 'mine/settings', element: withSuspense(<SettingsPage />) },
      { path: 'mine/account', element: withSuspense(<AccountPage />) },
      { path: 'mine/settings/chat', element: withSuspense(<ChatSettingsGlobal />) },
      { path: 'mine/settings/friend', element: withSuspense(<FriendSetting />) },
      { path: 'mine/settings/privacy', element: withSuspense(<PrivacySetting />) },
      { path: 'mine/settings/preference', element: withSuspense(<PreferenceSetting />) },
      { path: 'mine/about', element: withSuspense(<AboutPage />) },
      { path: 'create-post', element: withSuspense(<CreatePostPage />) },
      { path: 'create-mate', element: withSuspense(<CreateMatePage />) },
      { path: 'create-mate/:id', element: withSuspense(<CreateMatePage />) },
    ],
  },
  {
    element: <AuthLayout />,
    children: [
      { path: 'login', element: withSuspense(<LoginPage />) },
      { path: 'register', element: withSuspense(<RegisterPage />) },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);

export default router;
