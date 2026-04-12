import { useEffect, useState } from 'react';
import { planApi } from '../api/endpoints';
import { useStore } from '../store/useStore';
import { Layers, Plus, X, Check, Trash2 } from 'lucide-react';

interface Plan {
  planId: string;
  moduleName: string;
  planName: string;
  planLabel: string;
  description: string;
  features: string;
  sortOrder: number;
  isActive: boolean;
}

const MODULES = ['Operations', 'Reviewer', 'Publisher', 'EDMS', 'Planner', 'Support', 'Consulting'];

// Derive known features from the union of all plans for a module
// (the Enterprise plan should have everything — that becomes the feature registry)
function getKnownFeatures(plans: Plan[], moduleName: string): string[] {
  const allFeatures = new Set<string>();
  plans.filter((p) => p.moduleName === moduleName && p.isActive).forEach((p) => {
    try {
      const features: string[] = JSON.parse(p.features);
      features.forEach((f) => allFeatures.add(f));
    } catch {}
  });
  return Array.from(allFeatures);
}

export default function PlansPage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeModule, setActiveModule] = useState('Operations');
  const [editPlan, setEditPlan] = useState<Plan | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const hasPermission = useStore((s) => s.hasPermission);
  const canEdit = hasPermission('CONFIG_EDIT');

  useEffect(() => {
    planApi.list()
      .then((res) => setPlans(res.data.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const modulePlans = plans.filter((p) => p.moduleName === activeModule && p.isActive);
  const knownFeatures = getKnownFeatures(plans, activeModule);

  const getFeatures = (plan: Plan): string[] => {
    try { return JSON.parse(plan.features); } catch { return []; }
  };

  const toggleFeature = (plan: Plan, feature: string) => {
    const features = getFeatures(plan);
    const updated = features.includes(feature)
      ? features.filter((f) => f !== feature)
      : [...features, feature];
    handleUpdatePlan(plan.planId, { features: JSON.stringify(updated) });
  };

  const handleUpdatePlan = async (planId: string, data: any) => {
    try {
      const res = await planApi.update(planId, data);
      setPlans(plans.map((p) => p.planId === planId ? res.data.data : p));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update plan');
    }
  };

  const handleCreatePlan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editPlan) return;
    try {
      const res = await planApi.create({
        moduleName: activeModule,
        planName: editPlan.planName,
        planLabel: editPlan.planLabel,
        description: editPlan.description,
        features: editPlan.features || '[]',
        sortOrder: plans.filter((p) => p.moduleName === activeModule).length + 1,
      });
      setPlans([...plans, res.data.data]);
      setShowCreate(false);
      setEditPlan(null);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create plan');
    }
  };

  const handleDeletePlan = async (planId: string) => {
    if (!confirm('Deactivate this plan? Existing tenants on this plan will not be affected.')) return;
    try {
      await planApi.delete(planId);
      setPlans(plans.map((p) => p.planId === planId ? { ...p, isActive: false } : p));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to delete plan');
    }
  };

  if (loading) return <div className="text-center text-slate-500 py-12">Loading plans...</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Layers className="w-5 h-5 text-slate-600" />
          <h2 className="text-xl font-bold text-slate-900">Plan Management</h2>
        </div>
        {canEdit && (
          <button onClick={() => {
            setShowCreate(true);
            setEditPlan({ planId: '', moduleName: activeModule, planName: '', planLabel: '', description: '', features: '[]', sortOrder: 0, isActive: true });
          }}
            className="flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-sm font-medium hover:bg-slate-800 transition-colors">
            <Plus className="w-4 h-4" /> New Plan
          </button>
        )}
      </div>

      {/* Module tabs */}
      <div className="flex gap-2 mb-6 overflow-x-auto">
        {MODULES.map((mod) => {
          const count = plans.filter((p) => p.moduleName === mod && p.isActive).length;
          return (
            <button key={mod} onClick={() => setActiveModule(mod)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap ${
                activeModule === mod
                  ? 'bg-slate-900 text-white'
                  : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}>
              {mod} {count > 0 && <span className="ml-1 text-xs opacity-60">({count})</span>}
            </button>
          );
        })}
      </div>

      {/* Plans for selected module */}
      {modulePlans.length === 0 ? (
        <div className="text-center py-12 text-slate-500">
          No plans defined for {activeModule}. Click "New Plan" to create one.
        </div>
      ) : (
        <div className="space-y-4">
          {modulePlans.sort((a, b) => a.sortOrder - b.sortOrder).map((plan) => {
            const features = getFeatures(plan);
            return (
              <div key={plan.planId} className="bg-white rounded-xl border border-slate-200 overflow-hidden">
                <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
                  <div>
                    <h3 className="text-lg font-bold text-slate-900">{plan.planLabel}</h3>
                    <p className="text-sm text-slate-500 mt-0.5">{plan.description}</p>
                    <div className="text-xs text-slate-400 font-mono mt-1">ID: {plan.planName} | Sort: {plan.sortOrder}</div>
                  </div>
                  {canEdit && (
                    <div className="flex items-center gap-2">
                      <button onClick={() => handleDeletePlan(plan.planId)}
                        className="p-2 text-slate-400 hover:text-red-600 transition-colors" title="Deactivate">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  )}
                </div>

                <div className="px-6 py-4">
                  <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3">
                    Features ({features.length})
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {knownFeatures.map((f) => {
                      const included = features.includes(f);
                      return (
                        <button key={f} type="button"
                          onClick={() => canEdit && toggleFeature(plan, f)}
                          disabled={!canEdit}
                          className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border transition-all ${
                            included
                              ? 'bg-blue-50 border-blue-200 text-blue-700 hover:bg-blue-100'
                              : 'bg-slate-50 border-slate-200 text-slate-400 hover:border-slate-300'
                          }`}>
                          {included ? <Check className="w-3 h-3" /> : <X className="w-3 h-3" />}
                          {f}
                        </button>
                      );
                    })}

                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create Plan Modal */}
      {showCreate && editPlan && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6">
            <h3 className="text-lg font-bold text-slate-900 mb-4">New Plan for {activeModule}</h3>
            <form onSubmit={handleCreatePlan} className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Plan Name (key) *</label>
                <input type="text" required value={editPlan.planName}
                  onChange={(e) => setEditPlan({ ...editPlan, planName: e.target.value.toLowerCase().replace(/\s+/g, '-') })}
                  placeholder="e.g. premium"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Display Label *</label>
                <input type="text" required value={editPlan.planLabel}
                  onChange={(e) => setEditPlan({ ...editPlan, planLabel: e.target.value })}
                  placeholder="e.g. Premium"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Description</label>
                <input type="text" value={editPlan.description}
                  onChange={(e) => setEditPlan({ ...editPlan, description: e.target.value })}
                  placeholder="What's included in this plan"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => { setShowCreate(false); setEditPlan(null); }}
                  className="flex-1 py-2 border border-slate-300 rounded-lg text-sm text-slate-700 hover:bg-slate-50 transition-colors">Cancel</button>
                <button type="submit"
                  className="flex-1 py-2 bg-slate-900 text-white rounded-lg text-sm font-medium hover:bg-slate-800 transition-colors">Create Plan</button>
              </div>
            </form>
            <p className="text-xs text-slate-400 mt-3">After creating, toggle features on/off from the plan card.</p>
          </div>
        </div>
      )}
    </div>
  );
}
