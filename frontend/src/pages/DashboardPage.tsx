import { useEffect, useState } from 'react';
import { dashboardApi } from '../api/endpoints';
import type { DashboardOverview } from '../types';
import {
  Building2, Users, Key, Activity,
  Monitor, FileText, Database, Shield
} from 'lucide-react';

export default function DashboardPage() {
  const [data, setData] = useState<DashboardOverview | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardApi.overview()
      .then((res) => setData(res.data.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <div className="flex items-center justify-center h-64 text-slate-500">Loading dashboard...</div>;
  }

  if (!data) {
    return <div className="text-red-500">Failed to load dashboard data.</div>;
  }

  const stats = [
    { label: 'Active Tenants', value: data.activeTenants, total: data.totalTenants, icon: Building2, color: 'blue' },
    { label: 'Total Licenses', value: data.totalLicenses, icon: Key, color: 'green' },
    { label: 'Admin Users', value: data.activeAdminUsers, total: data.totalAdminUsers, icon: Users, color: 'purple' },
    { label: 'Audit Entries', value: data.totalAuditEntries, icon: Shield, color: 'amber' },
  ];

  const modules = [
    { name: 'Operations', count: data.operationsLicenses, icon: Activity, color: 'bg-blue-500' },
    { name: 'Reviewer', count: data.reviewerLicenses, icon: FileText, color: 'bg-emerald-500' },
    { name: 'Publisher', count: data.publisherLicenses, icon: Database, color: 'bg-purple-500' },
    { name: 'EDMS', count: data.edmsLicenses, icon: Monitor, color: 'bg-orange-500' },
    { name: 'Planner', count: data.plannerLicenses, icon: FileText, color: 'bg-pink-500' },
  ];

  const colorMap: Record<string, string> = {
    blue: 'bg-blue-50 text-blue-600 border-blue-200',
    green: 'bg-green-50 text-green-600 border-green-200',
    purple: 'bg-purple-50 text-purple-600 border-purple-200',
    amber: 'bg-amber-50 text-amber-600 border-amber-200',
  };

  return (
    <div>
      <h2 className="text-xl font-bold text-slate-900 mb-6">Platform Overview</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {stats.map((stat) => (
          <div key={stat.label} className={`rounded-xl border p-5 ${colorMap[stat.color]}`}>
            <div className="flex items-center justify-between mb-3">
              <stat.icon className="w-5 h-5" />
              {stat.total !== undefined && (
                <span className="text-xs opacity-60">of {stat.total}</span>
              )}
            </div>
            <div className="text-2xl font-bold">{stat.value}</div>
            <div className="text-sm mt-1 opacity-80">{stat.label}</div>
          </div>
        ))}
      </div>

      <h3 className="text-lg font-semibold text-slate-900 mb-4">Module Licenses</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        {modules.map((mod) => (
          <div key={mod.name} className="bg-white rounded-xl border border-slate-200 p-4">
            <div className="flex items-center gap-3 mb-3">
              <div className={`w-8 h-8 ${mod.color} rounded-lg flex items-center justify-center`}>
                <mod.icon className="w-4 h-4 text-white" />
              </div>
              <span className="font-medium text-slate-800">{mod.name}</span>
            </div>
            <div className="text-2xl font-bold text-slate-900">{mod.count}</div>
            <div className="text-xs text-slate-500">active licenses</div>
          </div>
        ))}
      </div>
    </div>
  );
}
