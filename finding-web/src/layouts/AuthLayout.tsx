import { Outlet } from 'react-router-dom';
import ToastContainer from '../components/Toast';
import './AuthLayout.css';

export default function AuthLayout() {
  return (
    <div className="auth-layout">
      <div className="auth-container">
        <div className="auth-brand">
          <h1>Finding</h1>
          <p>大学生专属社交平台</p>
        </div>
        <Outlet />
      </div>
      <ToastContainer />
    </div>
  );
}
