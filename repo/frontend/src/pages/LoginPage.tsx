import { useState } from 'react';
import { Button, Form, Input, Tabs, Typography, App as AntdApp } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';

export default function LoginPage() {
  const { login, register, user } = useAuth();
  const nav = useNavigate();
  const location = useLocation();
  const { message } = AntdApp.useApp();
  const [loading, setLoading] = useState(false);
  const [tab, setTab] = useState<'login' | 'register'>('login');

  if (user) {
    const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname || '/';
    nav(from, { replace: true });
    return null;
  }

  const onLogin = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      await login(values.username, values.password);
      message.success('Welcome back');
      nav('/', { replace: true });
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const onRegister = async (values: { username: string; password: string; displayName: string }) => {
    setLoading(true);
    try {
      await register(values.username, values.password, values.displayName);
      message.success('Account created');
      nav('/', { replace: true });
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-bg">
      <div className="login-card">
        <Typography.Title level={3} style={{ marginBottom: 4, textAlign: 'center' }}>
          City Bus Platform
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ textAlign: 'center', marginBottom: 24 }}>
          Sign in to access your dashboard
        </Typography.Paragraph>
        <Tabs
          activeKey={tab}
          onChange={(k) => setTab(k as 'login' | 'register')}
          centered
          items={[
            {
              key: 'login',
              label: 'Log in',
              children: (
                <Form layout="vertical" onFinish={onLogin}>
                  <Form.Item name="username" label="Username" rules={[{ required: true, message: 'Username required' }]}>
                    <Input placeholder="e.g. passenger1" autoComplete="username" />
                  </Form.Item>
                  <Form.Item name="password" label="Password" rules={[{ required: true, message: 'Password required' }]}>
                    <Input.Password placeholder="Your password" autoComplete="current-password" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block loading={loading}>
                    Log in
                  </Button>
                </Form>
              ),
            },
            {
              key: 'register',
              label: 'Register',
              children: (
                <Form layout="vertical" onFinish={onRegister}>
                  <Form.Item name="username" label="Username" rules={[{ required: true, message: 'Username required' }]}>
                    <Input placeholder="Choose a username" autoComplete="username" />
                  </Form.Item>
                  <Form.Item name="displayName" label="Display name">
                    <Input placeholder="Optional" autoComplete="name" />
                  </Form.Item>
                  <Form.Item
                    name="password"
                    label="Password"
                    rules={[
                      { required: true, message: 'Password required' },
                      { min: 8, message: 'Minimum 8 characters' },
                    ]}
                  >
                    <Input.Password placeholder="At least 8 characters" autoComplete="new-password" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block loading={loading}>
                    Create account
                  </Button>
                </Form>
              ),
            },
          ]}
        />
      </div>
    </div>
  );
}
