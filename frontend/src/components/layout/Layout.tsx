import { Outlet } from 'react-router-dom';
import { useStore } from '../../store/useStore';
import Sidebar from './Sidebar';
import Header from './Header';

export default function Layout() {
  const sidebarOpen = useStore((s) => s.sidebarOpen);

  return (
    <div className="flex h-screen bg-slate-50">
      <Sidebar />
      <div className={`flex-1 flex flex-col transition-all duration-200 ${sidebarOpen ? 'ml-64' : 'ml-16'}`}>
        <Header />
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
