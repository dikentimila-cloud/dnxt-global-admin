import { useEffect, useState } from 'react';
import { dashboardApi } from '../api/endpoints';
import { Activity, CheckCircle, XCircle, Server, Key, Building2 } from 'lucide-react';

export default function PlatformHealthPage() {
  const [health, setHealth] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const fetchHealth = () => {
    setLoading(true);
    dashboardApi.health()
      .then((res) => setHealth(res.data.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchHealth(); }, []);

  if (loading) return <div className="text-center text-slate-500 py-12">Loading platform health...</div>;
  if (!health) return <div className="text-red-500">Failed to load platform health.</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Activity className="w-5 h-5 text-slate-600" />
          <h2 className="text-xl font-bold text-slate-900">Platform Health</h2>
        </div>
        <button onClick={fetchHealth}
          className="px-3 py-1.5 text-sm border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors">
          Refresh
        </button>
      </div>

      {/* Service Status */}
      <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wide mb-3">Service Status</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
        {health.services?.map((svc: any) => (
          <div key={svc.name} className={`rounded-xl border p-4 ${
            svc.status === 'UP' ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'
          }`}>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <Server className={`w-4 h-4 ${svc.status === 'UP' ? 'text-green-600' : 'text-red-600'}`} />
                <span className="font-semibold text-slate-800">{svc.name}</span>
              </div>
              {svc.status === 'UP'
                ? <CheckCircle className="w-5 h-5 text-green-500" />
                : <XCircle className="w-5 h-5 text-red-500" />
              }
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className={svc.status === 'UP' ? 'text-green-700' : 'text-red-700'}>{svc.status}</span>
              <span className="text-slate-500">Port {svc.port}</span>
            </div>
          </div>
        ))}

        {/* Placeholder for future services */}
        {['Reviewer', 'Publisher', 'EDMS', 'Planner'].map((name) => (
          <div key={name} className="rounded-xl border border-slate-100 bg-slate-50 p-4 opacity-50">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <Server className="w-4 h-4 text-slate-400" />
                <span className="font-semibold text-slate-400">{name}</span>
              </div>
              <span className="text-[10px] bg-slate-200 text-slate-500 px-1.5 py-0.5 rounded">Not Connected</span>
            </div>
            <div className="text-sm text-slate-400">Pending integration</div>
          </div>
        ))}
      </div>

      {/* License Summary */}
      <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wide mb-3">License Summary</h3>
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden mb-8">
        <table className="w-full">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200">
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Module</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Active Licenses</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Status</th>
            </tr>
          </thead>
          <tbody>
            {health.licenseSummary && Object.entries(health.licenseSummary).map(([mod, data]: [string, any]) => (
              <tr key={mod} className="border-b border-slate-100">
                <td className="px-4 py-3">
                  <div className="flex items-center gap-2">
                    <Key className="w-4 h-4 text-slate-400" />
                    <span className="font-medium text-slate-800">{mod}</span>
                  </div>
                </td>
                <td className="px-4 py-3 text-sm font-semibold text-slate-900">{data.enabledCount}</td>
                <td className="px-4 py-3">
                  <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                    data.enabledCount > 0 ? 'bg-green-100 text-green-700' : 'bg-slate-100 text-slate-500'
                  }`}>
                    {data.enabledCount > 0 ? 'Active' : 'No Licenses'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Tenant Overview */}
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-white rounded-xl border border-slate-200 p-5">
          <div className="flex items-center gap-2 mb-2">
            <Building2 className="w-4 h-4 text-blue-500" />
            <span className="text-sm text-slate-500">Active Tenants</span>
          </div>
          <div className="text-3xl font-bold text-slate-900">{health.activeTenants}</div>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-5">
          <div className="flex items-center gap-2 mb-2">
            <Building2 className="w-4 h-4 text-slate-400" />
            <span className="text-sm text-slate-500">Total Tenants</span>
          </div>
          <div className="text-3xl font-bold text-slate-900">{health.totalTenants}</div>
        </div>
      </div>
    </div>
  );
}
