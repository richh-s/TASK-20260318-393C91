import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import PrivateRoute from './components/PrivateRoute';
import LoginPage from './pages/LoginPage';
import SearchPage from './pages/passenger/SearchPage';
import ReservationsPage from './pages/passenger/ReservationsPage';
import MessagesPage from './pages/passenger/MessagesPage';
import SettingsPage from './pages/passenger/SettingsPage';
import TaskListPage from './pages/dispatcher/TaskListPage';
import MyTasksPage from './pages/dispatcher/MyTasksPage';
import TaskDetailPage from './pages/dispatcher/TaskDetailPage';
import TemplatesPage from './pages/admin/TemplatesPage';
import WeightsPage from './pages/admin/WeightsPage';
import DictionariesPage from './pages/admin/DictionariesPage';
import ConfigsPage from './pages/admin/ConfigsPage';
import ImportsPage from './pages/admin/ImportsPage';
import { useAuth } from './store/AuthContext';

function RoleHomeRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.role === 'PASSENGER') return <Navigate to="/passenger/search" replace />;
  if (user.role === 'DISPATCHER') return <Navigate to="/dispatcher/tasks" replace />;
  if (user.role === 'ADMIN') return <Navigate to="/admin/templates" replace />;
  return <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <PrivateRoute>
            <AppLayout />
          </PrivateRoute>
        }
      >
        <Route path="/" element={<RoleHomeRedirect />} />
        <Route
          path="/passenger/search"
          element={<PrivateRoute roles={['PASSENGER']}><SearchPage /></PrivateRoute>}
        />
        <Route
          path="/passenger/reservations"
          element={<PrivateRoute roles={['PASSENGER']}><ReservationsPage /></PrivateRoute>}
        />
        <Route
          path="/passenger/messages"
          element={<PrivateRoute roles={['PASSENGER']}><MessagesPage /></PrivateRoute>}
        />
        <Route
          path="/passenger/settings"
          element={<PrivateRoute roles={['PASSENGER']}><SettingsPage /></PrivateRoute>}
        />
        <Route
          path="/dispatcher/tasks"
          element={<PrivateRoute roles={['DISPATCHER']}><TaskListPage /></PrivateRoute>}
        />
        <Route
          path="/dispatcher/my-tasks"
          element={<PrivateRoute roles={['DISPATCHER']}><MyTasksPage /></PrivateRoute>}
        />
        <Route
          path="/dispatcher/tasks/:id"
          element={<PrivateRoute roles={['DISPATCHER']}><TaskDetailPage /></PrivateRoute>}
        />
        <Route
          path="/admin/templates"
          element={<PrivateRoute roles={['ADMIN']}><TemplatesPage /></PrivateRoute>}
        />
        <Route
          path="/admin/weights"
          element={<PrivateRoute roles={['ADMIN']}><WeightsPage /></PrivateRoute>}
        />
        <Route
          path="/admin/dictionaries"
          element={<PrivateRoute roles={['ADMIN']}><DictionariesPage /></PrivateRoute>}
        />
        <Route
          path="/admin/configs"
          element={<PrivateRoute roles={['ADMIN']}><ConfigsPage /></PrivateRoute>}
        />
        <Route
          path="/admin/imports"
          element={<PrivateRoute roles={['ADMIN']}><ImportsPage /></PrivateRoute>}
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
