import { createBrowserRouter, Navigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import AuthLayout from '../layouts/AuthLayout';
import HomePage from '../pages/Home';
import SquarePage from '../pages/Square';
import MatePage from '../pages/Mate';
import MessagesPage from '../pages/Messages';
import MinePage from '../pages/Mine';
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
      { path: 'mine', element: <MinePage /> },
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
