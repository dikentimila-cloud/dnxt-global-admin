import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { tenantApi, planApi } from '../api/endpoints';
import { useStore } from '../store/useStore';
import type { Tenant } from '../types';
import { Building2, Plus, Search, ChevronRight } from 'lucide-react';

export default function TenantsPage() {
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const navigate = useNavigate();
  const hasPermission = useStore((s) => s.hasPermission);

  useEffect(() => {
    Promise.all([tenantApi.list(), planApi.list()])
      .then(([tenantsRes, plansRes]) => {
        setTenants(tenantsRes.data.data);
        setPlans(plansRes.data.data || []);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const filtered = tenants.filter((t) =>
    t.tenantName.toLowerCase().includes(search.toLowerCase()) ||
    t.tenantSlug.toLowerCase().includes(search.toLowerCase()) ||
    (t.industry || '').toLowerCase().includes(search.toLowerCase())
  );

  const allModules = [
    { name: 'Operations', available: true },
    { name: 'Reviewer', available: false },
    { name: 'Publisher', available: false },
    { name: 'EDMS', available: false },
    { name: 'Planner', available: false },
    { name: 'Support', available: false },
    { name: 'Consulting', available: false },
  ];

  const defaultModuleConfig = () => allModules.map((m) => ({
    moduleName: m.name,
    enabled: false,
    planId: '' as string,
    licenseType: 'Concurrent' as string,
    expiryDate: '',
    expiryDaysLeft: 365,
    licensePermits: 10,
  }));

  const [form, setForm] = useState({
    tenantName: '', primaryContactName: '', primaryContactEmail: '',
    industry: '', customerType: '', phone: '',
  });
  const [plans, setPlans] = useState<any[]>([]);
  const [moduleConfigs, setModuleConfigs] = useState(defaultModuleConfig());
  const [creating, setCreating] = useState(false);
  const [createResult, setCreateResult] = useState<any>(null);

  const toggleModuleEnabled = (moduleName: string) => {
    setModuleConfigs((prev) =>
      prev.map((m) => m.moduleName === moduleName ? { ...m, enabled: !m.enabled } : m)
    );
  };

  const updateModuleConfig = (moduleName: string, field: string, value: any) => {
    setModuleConfigs((prev) =>
      prev.map((m) => m.moduleName === moduleName ? { ...m, [field]: value } : m)
    );
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    setCreateResult(null);
    try {
      const res = await tenantApi.onboard({
        ...form,
        modules: moduleConfigs,
      });
      const data = res.data.data;
      if (data.tenant) {
        setTenants([...tenants, data.tenant]);
      }
      setCreateResult(data);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create tenant');
    } finally {
      setCreating(false);
    }
  };

  const closeCreateModal = () => {
    setShowCreate(false);
    setCreateResult(null);
    setForm({ tenantName: '', primaryContactName: '', primaryContactEmail: '', industry: '', customerType: '', phone: '' });
    setModuleConfigs(defaultModuleConfig());
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-bold text-slate-900">Tenants</h2>
        {hasPermission('TENANT_CREATE') && (
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-sm font-medium hover:bg-slate-800 transition-colors"
          >
            <Plus className="w-4 h-4" /> New Tenant
          </button>
        )}
      </div>

      <div className="relative mb-4">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
        <input
          type="text"
          placeholder="Search tenants..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {loading ? (
        <div className="text-center text-slate-500 py-12">Loading tenants...</div>
      ) : (
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200">
                <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Tenant</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Slug</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Industry</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Contact</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Status</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">License</th>
                <th className="w-10"></th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((t) => (
                <tr
                  key={t.tenantId}
                  onClick={() => navigate(`/tenants/${t.tenantId}`)}
                  className="border-b border-slate-100 hover:bg-slate-50 cursor-pointer transition-colors"
                >
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <Building2 className="w-4 h-4 text-slate-400" />
                      <span className="font-medium text-slate-800">{t.tenantName}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-600 font-mono">{t.tenantSlug}</td>
                  <td className="px-4 py-3 text-sm text-slate-600">{t.industry || '-'}</td>
                  <td className="px-4 py-3 text-sm text-slate-600">{t.primaryContactEmail}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                      t.status === 'Active' ? 'bg-green-100 text-green-700' : 'bg-slate-100 text-slate-600'
                    }`}>
                      {t.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-600">{t.licenseType || '-'}</td>
                  <td className="px-4 py-3">
                    <ChevronRight className="w-4 h-4 text-slate-400" />
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr><td colSpan={7} className="text-center py-8 text-slate-500">No tenants found.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Create Tenant Modal */}
      {showCreate && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-y-auto py-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl mx-4 p-6 max-h-[90vh] overflow-y-auto">
            {!createResult ? (
              <>
                <h3 className="text-lg font-bold text-slate-900 mb-1">Onboard New Tenant</h3>
                <p className="text-sm text-slate-500 mb-4">Creates the tenant, configures module licensing, and provisions their environment.</p>
                <form onSubmit={handleCreate} className="space-y-4">
                  {/* Tenant Info */}
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">Organization Name *</label>
                    <input type="text" required value={form.tenantName} onChange={(e) => setForm({ ...form, tenantName: e.target.value })}
                      placeholder="e.g. DnXT Solutions, Pfizer, Takeda"
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">Contact Name *</label>
                      <input type="text" required value={form.primaryContactName} onChange={(e) => setForm({ ...form, primaryContactName: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">Contact Email *</label>
                      <input type="email" required value={form.primaryContactEmail} onChange={(e) => setForm({ ...form, primaryContactEmail: e.target.value })}
                        placeholder="admin@company.com"
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">Customer Type</label>
                      <select value={form.customerType} onChange={(e) => setForm({ ...form, customerType: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                        <option value="">Select...</option>
                        <option value="Pharma">Pharma</option>
                        <option value="Biotech">Biotech</option>
                        <option value="MedDevice">Medical Device</option>
                        <option value="CRO">CRO</option>
                        <option value="Enterprise">Enterprise</option>
                        <option value="Other">Other</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">Industry</label>
                      <input type="text" value={form.industry} onChange={(e) => setForm({ ...form, industry: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                    </div>
                  </div>

                  {/* License Configuration */}
                  <div>
                    <h4 className="text-sm font-semibold text-blue-700 uppercase tracking-wide mb-3">License Configuration</h4>
                    <div className="space-y-2">
                      {allModules.map((mod) => {
                        const config = moduleConfigs.find((m) => m.moduleName === mod.name)!;
                        return (
                          <div key={mod.name} className="border border-slate-200 rounded-lg overflow-hidden">
                            <div className="flex items-center justify-between px-4 py-2.5 bg-slate-50">
                              <div className="flex items-center gap-2">
                                <div className={`w-2 h-2 rounded-full ${config.enabled ? 'bg-green-500' : 'bg-slate-300'}`} />
                                <span className={`text-sm font-semibold ${config.enabled ? 'text-blue-700' : 'text-slate-600'}`}>
                                  {mod.name.toUpperCase()}
                                </span>
                                {!mod.available && <span className="text-[10px] bg-slate-200 text-slate-500 px-1.5 py-0.5 rounded">Coming Soon</span>}
                              </div>
                              <button
                                type="button"
                                onClick={() => mod.available && toggleModuleEnabled(mod.name)}
                                disabled={!mod.available}
                                className={`text-xs font-medium px-3 py-1 rounded transition-colors ${
                                  config.enabled
                                    ? 'text-red-600 hover:bg-red-50'
                                    : mod.available
                                      ? 'text-blue-600 hover:bg-blue-50'
                                      : 'text-slate-400 cursor-not-allowed'
                                }`}
                              >
                                {config.enabled ? 'CANCEL' : 'ACTIVATE'}
                              </button>
                            </div>
                            {config.enabled && (
                              <div className="px-4 py-3 space-y-3">
                                {/* Plan Selector */}
                                <div>
                                  <label className="block text-xs font-medium text-slate-500 mb-1">PLAN *</label>
                                  <select value={config.planId}
                                    onChange={(e) => updateModuleConfig(mod.name, 'planId', e.target.value)}
                                    className="w-full px-2 py-1.5 border border-slate-300 rounded text-sm focus:outline-none focus:ring-1 focus:ring-blue-500">
                                    <option value="">Select plan...</option>
                                    {plans.filter((p: any) => p.moduleName === mod.name).map((p: any) => (
                                      <option key={p.planId} value={p.planId}>{p.planLabel} — {p.description}</option>
                                    ))}
                                  </select>
                                  {config.planId && (() => {
                                    const plan = plans.find((p: any) => p.planId === config.planId);
                                    if (!plan) return null;
                                    const features = JSON.parse(plan.features || '[]');
                                    return (
                                      <div className="mt-1.5 flex flex-wrap gap-1">
                                        {features.map((f: string) => (
                                          <span key={f} className="inline-flex px-1.5 py-0.5 bg-blue-50 text-blue-700 rounded text-[10px] font-medium">{f}</span>
                                        ))}
                                      </div>
                                    );
                                  })()}
                                </div>
                                {/* License Config */}
                                <div className="grid grid-cols-4 gap-3">
                                <div>
                                  <label className="block text-xs font-medium text-slate-500 mb-1">LICENSE TYPE *</label>
                                  <select value={config.licenseType}
                                    onChange={(e) => updateModuleConfig(mod.name, 'licenseType', e.target.value)}
                                    className="w-full px-2 py-1.5 border border-slate-300 rounded text-sm focus:outline-none focus:ring-1 focus:ring-blue-500">
                                    <option value="Concurrent">Concurrent</option>
                                    <option value="Named">Named</option>
                                  </select>
                                </div>
                                <div>
                                  <label className="block text-xs font-medium text-slate-500 mb-1">LICENSE EXPIRY DATE *</label>
                                  <input type="date" value={config.expiryDate}
                                    onChange={(e) => updateModuleConfig(mod.name, 'expiryDate', e.target.value)}
                                    className="w-full px-2 py-1.5 border border-slate-300 rounded text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
                                </div>
                                <div>
                                  <label className="block text-xs font-medium text-slate-500 mb-1">EXPIRY DAYS LEFT *</label>
                                  <input type="number" min="1" value={config.expiryDaysLeft}
                                    onChange={(e) => updateModuleConfig(mod.name, 'expiryDaysLeft', parseInt(e.target.value))}
                                    className="w-full px-2 py-1.5 border border-slate-300 rounded text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
                                </div>
                                <div>
                                  <label className="block text-xs font-medium text-slate-500 mb-1">LICENSE PERMITS *</label>
                                  <input type="number" min="1" value={config.licensePermits}
                                    onChange={(e) => updateModuleConfig(mod.name, 'licensePermits', parseInt(e.target.value))}
                                    className="w-full px-2 py-1.5 border border-slate-300 rounded text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
                                </div>
                                </div>
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  <div className="bg-slate-50 rounded-lg p-3 text-xs text-slate-600">
                    <strong>What happens:</strong> A tenant record is created, selected modules are licensed, and for Operations — a user account is auto-created with the contact email + temporary password.
                  </div>

                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={closeCreateModal}
                      className="flex-1 py-2 border border-slate-300 rounded-lg text-sm text-slate-700 hover:bg-slate-50 transition-colors">Cancel</button>
                    <button type="submit" disabled={creating}
                      className="flex-1 py-2 bg-slate-900 text-white rounded-lg text-sm font-medium hover:bg-slate-800 disabled:opacity-50 transition-colors">
                      {creating ? 'Provisioning...' : 'Create & Provision'}
                    </button>
                  </div>
                </form>
              </>
            ) : (
              /* Provisioning Result */
              <div>
                <div className="flex items-center gap-2 mb-4">
                  <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                    <svg className="w-5 h-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-900">Tenant Created</h3>
                    <p className="text-sm text-slate-500">{createResult.tenant?.tenantName} ({createResult.tenant?.tenantSlug})</p>
                  </div>
                </div>

                {createResult.provisioning?.operations && (
                  <div className="mb-4">
                    <h4 className="text-sm font-medium text-slate-700 mb-2">Operations Provisioning</h4>
                    <div className={`rounded-lg p-3 text-sm ${
                      createResult.provisioning.operations.status === 'provisioned'
                        ? 'bg-green-50 text-green-700 border border-green-200'
                        : 'bg-red-50 text-red-700 border border-red-200'
                    }`}>
                      {createResult.provisioning.operations.status === 'provisioned' ? (
                        <div>
                          <div className="font-medium">Successfully provisioned in Operations</div>
                          {createResult.provisioning.operations.operationsResult?.tempPassword && (
                            <div className="mt-2 bg-white rounded p-2 border border-green-200">
                              <div className="text-xs text-slate-500">Temporary Password (share with tenant admin):</div>
                              <div className="font-mono font-bold text-green-800 select-all">{createResult.provisioning.operations.operationsResult.tempPassword}</div>
                            </div>
                          )}
                          {createResult.provisioning.operations.operationsResult?.username && (
                            <div className="text-xs mt-1">Username: <strong>{createResult.provisioning.operations.operationsResult.username}</strong></div>
                          )}
                        </div>
                      ) : (
                        <div>
                          <div className="font-medium">Provisioning failed</div>
                          <div className="text-xs mt-1">{createResult.provisioning.operations.error}</div>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                <div className="bg-slate-50 rounded-lg p-3 text-xs text-slate-600 mb-4">
                  The tenant can now log in at <strong>login.dnxtcloud.com</strong> and will be redirected to <strong>{createResult.tenant?.tenantSlug}-operations.dnxtcloud.com</strong>
                </div>

                <button onClick={closeCreateModal}
                  className="w-full py-2 bg-slate-900 text-white rounded-lg text-sm font-medium hover:bg-slate-800 transition-colors">Done</button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
