import { useEffect, useState } from 'react';
import { configApi } from '../api/endpoints';
import { useStore } from '../store/useStore';
import type { PlatformConfig } from '../types';
import { Settings, Save, Shield, Mail, Globe } from 'lucide-react';

const categoryIcons: Record<string, any> = {
  SECURITY: Shield,
  MAIL: Mail,
  GENERAL: Globe,
};

export default function SettingsPage() {
  const [configs, setConfigs] = useState<PlatformConfig[]>([]);
  const [edited, setEdited] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState('GENERAL');
  const hasPermission = useStore((s) => s.hasPermission);
  const canEdit = hasPermission('CONFIG_EDIT');

  useEffect(() => {
    configApi.list()
      .then((res) => setConfigs(res.data.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const categories = [...new Set(configs.map((c) => c.category))];
  const filtered = configs.filter((c) => c.category === activeTab);

  const handleSave = async () => {
    if (Object.keys(edited).length === 0) return;
    setSaving(true);
    try {
      await configApi.update(edited);
      setConfigs(configs.map((c) =>
        edited[c.configKey] !== undefined
          ? { ...c, configValue: edited[c.configKey] }
          : c
      ));
      setEdited({});
      alert('Settings saved.');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="text-center text-slate-500 py-12">Loading...</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-bold text-slate-900">Platform Settings</h2>
        {canEdit && Object.keys(edited).length > 0 && (
          <button onClick={handleSave} disabled={saving}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors">
            <Save className="w-4 h-4" /> {saving ? 'Saving...' : `Save ${Object.keys(edited).length} Changes`}
          </button>
        )}
      </div>

      <div className="flex gap-2 mb-4">
        {categories.map((cat) => {
          const Icon = categoryIcons[cat] || Settings;
          return (
            <button
              key={cat}
              onClick={() => setActiveTab(cat)}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                activeTab === cat
                  ? 'bg-slate-900 text-white'
                  : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}
            >
              <Icon className="w-4 h-4" /> {cat}
            </button>
          );
        })}
      </div>

      <div className="bg-white rounded-xl border border-slate-200 divide-y divide-slate-100">
        {filtered.map((config) => (
          <div key={config.configKey} className="px-6 py-4">
            <div className="flex items-center justify-between">
              <div className="flex-1 mr-8">
                <div className="text-sm font-medium text-slate-800">{config.configKey}</div>
                {config.description && (
                  <div className="text-xs text-slate-500 mt-0.5">{config.description}</div>
                )}
              </div>
              <div className="w-80">
                {config.isSecret ? (
                  <input
                    type="password"
                    value={edited[config.configKey] ?? config.configValue}
                    onChange={(e) => setEdited({ ...edited, [config.configKey]: e.target.value })}
                    disabled={!canEdit}
                    className="w-full px-3 py-1.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-slate-50"
                  />
                ) : (
                  <input
                    type="text"
                    value={edited[config.configKey] ?? config.configValue}
                    onChange={(e) => setEdited({ ...edited, [config.configKey]: e.target.value })}
                    disabled={!canEdit}
                    className="w-full px-3 py-1.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-slate-50"
                  />
                )}
              </div>
            </div>
          </div>
        ))}
        {filtered.length === 0 && (
          <div className="text-center py-8 text-slate-500">No settings in this category.</div>
        )}
      </div>
    </div>
  );
}
