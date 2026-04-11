import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { tenantApi } from '../api/endpoints';
import { useStore } from '../store/useStore';
import type { Tenant, TenantModule } from '../types';
import { ArrowLeft, Building2, Power, ToggleLeft, ToggleRight, ExternalLink, Loader2 } from 'lucide-react';

export default function TenantDetailPage() {
  const { tenantId } = useParams<{ tenantId: string }>();
  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [modules, setModules] = useState<TenantModule[]>([]);
  const [loading, setLoading] = useState(true);
  const [accessLoading, setAccessLoading] = useState(false);
  const navigate = useNavigate();
  const hasPermission = useStore((s) => s.hasPermission);

  useEffect(() => {
    if (!tenantId) return;
    tenantApi.get(tenantId)
      .then((res) => {
        setTenant(res.data.data.tenant);
        setModules(res.data.data.modules);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [tenantId]);

  const handleToggleModule = async (moduleName: string, currentEnabled: boolean) => {
    if (!tenantId) return;
    try {
      await tenantApi.toggleModule(tenantId, moduleName, !currentEnabled);
      setModules(modules.map((m) =>
        m.moduleName === moduleName ? { ...m, isEnabled: !currentEnabled } : m
      ));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to toggle module');
    }
  };

  const handleDeactivate = async () => {
    if (!tenantId || !confirm('Are you sure? This will disable all modules for this tenant.')) return;
    try {
      await tenantApi.deactivate(tenantId);
      setTenant((t) => t ? { ...t, status: 'Inactive', isActive: false } : null);
      setModules(modules.map((m) => ({ ...m, isEnabled: false })));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to deactivate');
    }
  };

  const handleReactivate = async () => {
    if (!tenantId) return;
    try {
      await tenantApi.reactivate(tenantId);
      setTenant((t) => t ? { ...t, status: 'Active', isActive: true } : null);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to reactivate');
    }
  };

  const handleAccessTenant = async () => {
    if (!tenantId) return;
    setAccessLoading(true);
    try {
      const res = await tenantApi.supportAccess(tenantId);
      const { redirectUrl } = res.data.data;
      window.open(redirectUrl, '_blank');
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to initiate support access';
      alert(msg);
    } finally {
      setAccessLoading(false);
    }
  };

  if (loading) return <div className="text-center text-slate-500 py-12">Loading...</div>;
  if (!tenant) return <div className="text-red-500">Tenant not found.</div>;

  const opsEnabled = modules.some((m) => m.moduleName === 'Operations' && m.isEnabled);

  const moduleColors: Record<string, string> = {
    Operations: 'bg-blue-500',
    Reviewer: 'bg-emerald-500',
    Publisher: 'bg-purple-500',
    EDMS: 'bg-orange-500',
    Planner: 'bg-pink-500',
  };

  return (
    <div>
      <button onClick={() => navigate('/tenants')} className="flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700 mb-4">
        <ArrowLeft className="w-4 h-4" /> Back to Tenants
      </button>

      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 bg-slate-100 rounded-xl flex items-center justify-center">
            <Building2 className="w-6 h-6 text-slate-600" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-slate-900">{tenant.tenantName}</h2>
            <span className="text-sm text-slate-500 font-mono">{tenant.tenantSlug}</span>
          </div>
          <span className={`ml-3 inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium ${
            tenant.status === 'Active' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
          }`}>{tenant.status}</span>
        </div>
        <div className="flex items-center gap-2">
          {/* Access Tenant (Login-As) */}
          {hasPermission('SUPPORT_ACCESS') && tenant.isActive && opsEnabled && (
            <button
              onClick={handleAccessTenant}
              disabled={accessLoading}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {accessLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <ExternalLink className="w-4 h-4" />}
              {accessLoading ? 'Connecting...' : 'Access Tenant'}
            </button>
          )}

          {hasPermission('TENANT_DEACTIVATE') && (
            tenant.isActive ? (
              <button onClick={handleDeactivate} className="flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-lg text-sm hover:bg-red-700 transition-colors">
                <Power className="w-4 h-4" /> Deactivate
              </button>
            ) : (
              <button onClick={handleReactivate} className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg text-sm hover:bg-green-700 transition-colors">
                <Power className="w-4 h-4" /> Reactivate
              </button>
            )
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Tenant Details */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 p-6">
          <h3 className="font-semibold text-slate-900 mb-4">Details</h3>
          <div className="grid grid-cols-2 gap-4 text-sm">
            {[
              ['Contact', tenant.primaryContactName],
              ['Email', tenant.primaryContactEmail],
              ['Phone', tenant.phone || '-'],
              ['Industry', tenant.industry || '-'],
              ['Domain', tenant.domain || '-'],
              ['License Type', tenant.licenseType || '-'],
              ['License Expiry', tenant.licenseExpiry || '-'],
              ['Max Users', String(tenant.maxUsers)],
            ].map(([label, value]) => (
              <div key={label}>
                <div className="text-slate-500 text-xs uppercase tracking-wide mb-0.5">{label}</div>
                <div className="text-slate-800 font-medium">{value}</div>
              </div>
            ))}
          </div>
          {tenant.notes && (
            <div className="mt-4 pt-4 border-t border-slate-100">
              <div className="text-slate-500 text-xs uppercase tracking-wide mb-1">Notes</div>
              <div className="text-sm text-slate-700">{tenant.notes}</div>
            </div>
          )}
        </div>

        {/* Module Licensing */}
        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h3 className="font-semibold text-slate-900 mb-4">Module Licensing</h3>
          <div className="space-y-3">
            {modules.map((mod) => (
              <div key={mod.moduleId} className="flex items-center justify-between py-2 border-b border-slate-100 last:border-0">
                <div className="flex items-center gap-2">
                  <div className={`w-2 h-2 rounded-full ${mod.isEnabled ? moduleColors[mod.moduleName] || 'bg-slate-400' : 'bg-slate-300'}`} />
                  <span className={`text-sm font-medium ${mod.isEnabled ? 'text-slate-800' : 'text-slate-400'}`}>
                    {mod.moduleName}
                  </span>
                </div>
                {hasPermission('MODULE_MANAGE') && (
                  <button
                    onClick={() => handleToggleModule(mod.moduleName, mod.isEnabled)}
                    className="text-slate-400 hover:text-slate-600 transition-colors"
                  >
                    {mod.isEnabled
                      ? <ToggleRight className="w-6 h-6 text-blue-500" />
                      : <ToggleLeft className="w-6 h-6" />
                    }
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
