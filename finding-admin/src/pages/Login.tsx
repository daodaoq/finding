import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import axios from 'axios';
import { adminTokenStorage } from '../utils/adminTokenStorage';

export default function Login() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  // 被守卫拦下时携带的原目标地址,登录后回跳
  const from = (location.state as { from?: string } | null)?.from || '/dashboard';

  // 已登录则直接跳转(优先回原目标);token 过期会被 getValid 自动清除
  useEffect(() => {
    if (adminTokenStorage.getValid()) {
      navigate(from, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [navigate]);

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const res = await axios.post('/api/v1/auth/login', {
        phone: values.username.trim(),
        password: values.password.trim(),
        loginType: 'password',
      });
      const token = res.data?.data?.accessToken;
      if (token) {
        adminTokenStorage.set(token);
        message.success('登录成功');
        navigate(from, { replace: true });
      } else {
        message.error('登录失败，未获取到令牌');
      }
    } catch {
      message.error('登录失败，请检查账号密码');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center',
      justifyContent: 'center', background: 'linear-gradient(135deg, #ff6b81, #ff9a76)',
    }}>
      <Card
        style={{ width: 400 }}
        title={<div style={{ textAlign: 'center', fontSize: 20, color: '#ff6b81' }}>Finding 后台管理</div>}
      >
        <Form onFinish={onFinish} size="large">
          <Form.Item name="username" rules={[{ required: true, message: '请输入管理员账号' }]}>
            <Input prefix={<UserOutlined />} placeholder="管理员手机号" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>
        </Form>
        <div style={{ textAlign: 'center', color: '#999', fontSize: 12 }}>
          仅限管理员账号登录
        </div>
      </Card>
    </div>
  );
}
