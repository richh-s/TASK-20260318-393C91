import { Layout, Menu, Button, Space, Typography, Tag } from 'antd';
import { Outlet, useLocation, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';
import { UserRole } from '../types';

const { Header, Content } = Layout;

const menuByRole: Record<UserRole, { key: string; label: string }[]> = {
  PASSENGER: [
    { key: '/passenger/search', label: 'Search' },
    { key: '/passenger/reservations', label: 'Reservations' },
    { key: '/passenger/messages', label: 'Messages' },
    { key: '/passenger/settings', label: 'Settings' },
  ],
  DISPATCHER: [
    { key: '/dispatcher/tasks', label: 'Tasks' },
    { key: '/dispatcher/my-tasks', label: 'My Tasks' },
  ],
  ADMIN: [
    { key: '/admin/templates', label: 'Templates' },
    { key: '/admin/weights', label: 'Sorting Weights' },
    { key: '/admin/dictionaries', label: 'Field Dictionary' },
    { key: '/admin/configs', label: 'System Config' },
    { key: '/admin/imports', label: 'Bus Data Imports' },
  ],
};

export default function AppLayout() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const location = useLocation();

  if (!user) return null;

  const items = menuByRole[user.role] || [];
  const selected = items.find((i) => location.pathname.startsWith(i.key))?.key;

  const onLogout = () => {
    logout();
    nav('/login');
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', background: '#001529', padding: '0 24px' }}>
        <Link to="/" style={{ color: '#fff', fontSize: 18, fontWeight: 600, marginRight: 40 }}>
          City Bus Platform
        </Link>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={selected ? [selected] : []}
          onClick={({ key }) => nav(key)}
          items={items.map((i) => ({ key: i.key, label: i.label }))}
          style={{ flex: 1, minWidth: 0 }}
        />
        <Space>
          <Tag color={user.role === 'ADMIN' ? 'red' : user.role === 'DISPATCHER' ? 'blue' : 'green'}>
            {user.role}
          </Tag>
          <Typography.Text style={{ color: '#fff' }}>{user.displayName || user.username}</Typography.Text>
          <Button size="small" onClick={onLogout}>Log out</Button>
        </Space>
      </Header>
      <Content>
        <Outlet />
      </Content>
    </Layout>
  );
}
