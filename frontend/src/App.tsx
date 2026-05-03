import { Routes, Route, Navigate } from 'react-router-dom';
import { useStore } from './store/useStore';
import LoginPage from './pages/LoginPage';
import Layout from './components/layout/Layout';
import DashboardPage from './pages/DashboardPage';
import TenantsPage from './pages/TenantsPage';
import TenantDetailPage from './pages/TenantDetailPage';
import UsersPage from './pages/UsersPage';
import SettingsPage from './pages/SettingsPage';
import AuditPage from './pages/AuditPage';
import PlatformHealthPage from './pages/PlatformHealthPage';
import ChangePasswordPage from './pages/ChangePasswordPage';
import PlansPage from './pages/PlansPage';
import AiConfigPage from './pages/AiConfigPage';
import ReleasesPage from './pages/ReleasesPage';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = useStore((s) => s.token);
  const user = useStore((s) => s.user);

  if (!token) return <Navigate to="/login" replace />;

  // Force password change on first login
  if (user?.mustChangePassword && window.location.pathname !== '/change-password') {
    return <Navigate to="/change-password" replace />;
  }

  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/change-password" element={
        <ProtectedRoute><ChangePasswordPage /></ProtectedRoute>
      } />
      <Route path="/" element={
        <ProtectedRoute><Layout /></ProtectedRoute>
      }>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="tenants" element={<TenantsPage />} />
        <Route path="tenants/:tenantId" element={<TenantDetailPage />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="audit" element={<AuditPage />} />
        <Route path="health" element={<PlatformHealthPage />} />
        <Route path="ai-config" element={<AiConfigPage />} />
        <Route path="plans" element={<PlansPage />} />
        <Route path="releases" element={<ReleasesPage />} />
      </Route>
    </Routes>
  );
}
