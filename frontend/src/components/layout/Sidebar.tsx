import { NavLink } from 'react-router-dom';
import { useStore } from '../../store/useStore';
import {
  LayoutDashboard, Building2, Users, Settings, Shield, Activity, Layers,
  ChevronLeft, ChevronRight, Globe, Brain
} from 'lucide-react';

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard', permission: 'DASHBOARD_VIEW' },
  { to: '/tenants', icon: Building2, label: 'Tenants', permission: 'TENANT_VIEW' },
  { to: '/plans', icon: Layers, label: 'Plans', permission: 'CONFIG_VIEW' },
  { to: '/users', icon: Users, label: 'Admin Users', permission: 'USER_VIEW' },
  { to: '/health', icon: Activity, label: 'Platform Health', permission: 'DASHBOARD_VIEW' },
  { to: '/ai-config', icon: Brain, label: 'AI Configuration', permission: 'CONFIG_EDIT' },
  { to: '/settings', icon: Settings, label: 'Settings', permission: 'CONFIG_VIEW' },
  { to: '/audit', icon: Shield, label: 'Audit Log', permission: 'AUDIT_VIEW' },
];

export default function Sidebar() {
  const sidebarOpen = useStore((s) => s.sidebarOpen);
  const toggleSidebar = useStore((s) => s.toggleSidebar);
  const hasPermission = useStore((s) => s.hasPermission);

  return (
    <aside className={`fixed left-0 top-0 h-full bg-slate-900 text-white transition-all duration-200 z-30 ${sidebarOpen ? 'w-64' : 'w-16'}`}>
      <div className="flex items-center h-16 px-4 border-b border-slate-700">
        <Globe className="w-7 h-7 text-blue-400 shrink-0" />
        {sidebarOpen && (
          <div className="ml-3">
            <div className="font-bold text-sm leading-tight">DnXT Global</div>
            <div className="text-[10px] text-slate-400 uppercase tracking-wider">Admin Portal</div>
          </div>
        )}
      </div>

      <nav className="mt-4 flex flex-col gap-1 px-2">
        {navItems.map((item) => {
          if (!hasPermission(item.permission)) return null;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors ${
                  isActive
                    ? 'bg-blue-600 text-white'
                    : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                }`
              }
            >
              <item.icon className="w-5 h-5 shrink-0" />
              {sidebarOpen && <span>{item.label}</span>}
            </NavLink>
          );
        })}
      </nav>

      <button
        onClick={toggleSidebar}
        className="absolute bottom-4 left-0 right-0 flex justify-center text-slate-400 hover:text-white transition-colors"
      >
        {sidebarOpen ? <ChevronLeft className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
      </button>
    </aside>
  );
}
