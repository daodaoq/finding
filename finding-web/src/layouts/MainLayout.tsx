import { Outlet } from 'react-router-dom';
import BottomNav from '../components/BottomNav';
import ToastContainer from '../components/Toast';
import './MainLayout.css';

export default function MainLayout() {
  return (
    <div className="main-layout">
      <div className="main-content">
        <Outlet />
      </div>
      <BottomNav />
      <ToastContainer />
    </div>
  );
}
