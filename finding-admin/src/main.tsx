import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// 路由 basename 为 /admin:直接访问根路径时无匹配路由会白屏,统一跳转到 /admin
if (window.location.pathname === '/') {
  window.location.replace('/admin');
} else {
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode><App /></React.StrictMode>
  );
}
