import { useEffect, useState } from 'react';
import { configApi } from '../api/endpoints';
import { useStore } from '../store/useStore';
import type { PlatformConfig } from '../types';
import { Settings, Save, Shield, Mail, Globe, KeyRound, ToggleLeft, ToggleRight } from 'lucide-react';

const categoryIcons: Record<string, any> = {
  SECURITY: Shield,
  MAIL: Mail,
  GENERAL: Globe,
  SSO: KeyRound,
};

const categoryLabels: Record<string, string> = {
  GENERAL: 'General',
  SECURITY: 'Security',
  MAIL: 'Mail',
  SSO: 'Single Sign-On',
};

const categoryOrder = ['GENERAL', 'SECURITY', 'SSO', 'MAIL'];

/** Keys that render as on/off toggles */
const booleanKeys = new Set([
  'sso.enabled', 'sso.password_fallback', 'sso.auto_create_users',
]);

/** Keys that render as dropdowns */
const selectOptions: Record<string, { value: string; label: string }[]> = {
  'sso.provider': [
    { value: 'google', label: 'Google Workspace' },
    { value: 'azure_ad', label: 'Azure AD (Coming Soon)' },
    { value: 'okta', label: 'Okta (Coming Soon)' },
    { value: 'saml', label: 'SAML 2.0 (Coming Soon)' },
  ],
  'sso.google.prompt': [
    { value: 'consent', label: 'Consent (re-authorize each time)' },
    { value: 'select_account', label: 'Select Account' },
    { value: 'none', label: 'None (silent)' },
  ],
};

/** Friendly display names for config keys */
const keyLabels: Record<string, string> = {
  'sso.enabled': 'SSO Enabled',
  'sso.provider': 'SSO Provider',
  'sso.google.client_id': 'Google Client ID',
  'sso.google.client_secret': 'Google Client Secret',
  'sso.google.redirect_uri': 'Redirect URI',
  'sso.google.hosted_domain': 'Hosted Domain (hd)',
  'sso.google.scopes': 'OAuth Scopes',
  'sso.google.prompt': 'Prompt Behavior',
  'sso.password_fallback': 'Allow Password Fallback',
  'sso.auto_create_users': 'Auto-Create Users on SSO',
};

function ConfigField({ config, value, canEdit, onChange }: {
  config: PlatformConfig;
  value: string;
  canEdit: boolean;
  onChange: (val: string) => void;
}) {
  const key = config.configKey;

  // Boolean toggle
  if (booleanKeys.has(key)) {
    const isOn = value === 'true';
    return (
      <button
        onClick={() => canEdit && onChange(isOn ? 'false' : 'true')}
        disabled={!canEdit}
        className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
          isOn
            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
            : 'bg-slate-50 text-slate-500 border border-slate-200'
        } ${canEdit ? 'cursor-pointer hover:shadow-sm' : 'opacity-60 cursor-not-allowed'}`}
      >
        {isOn ? <ToggleRight className="w-5 h-5" /> : <ToggleLeft className="w-5 h-5" />}
        {isOn ? 'Enabled' : 'Disabled'}
      </button>
    );
  }

  // Dropdown select
  if (selectOptions[key]) {
    return (
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={!canEdit}
        className="w-full px-3 py-1.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-slate-50 bg-white"
      >
        {selectOptions[key].map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    );
  }

  // Secret field
  if (config.isSecret) {
    return (
      <input
        type="password"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={!canEdit}
        placeholder="Enter secret value..."
        className="w-full px-3 py-1.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-slate-50"
      />
    );
  }

  // Regular text input
  return (
    <input
      type="text"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      disabled={!canEdit}
      className="w-full px-3 py-1.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-slate-50"
    />
  );
}

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

  const categories = categoryOrder.filter((cat) =>
    configs.some((c) => c.category === cat)
  );
  // Include any categories from DB that aren't in the predefined order
  configs.forEach((c) => {
    if (!categories.includes(c.category)) categories.push(c.category);
  });

  const filtered = configs.filter((c) => c.category === activeTab);

  const aiFeatures: PlatformConfig[] = [];
  const aiOther = filtered;

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

  const renderConfigRow = (config: PlatformConfig) => {
    const label = keyLabels[config.configKey] || config.configKey;
    const currentValue = edited[config.configKey] ?? config.configValue;

    return (
      <div key={config.configKey} className="px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex-1 mr-8">
            <div className="text-sm font-medium text-slate-800">{label}</div>
            {config.description && (
              <div className="text-xs text-slate-500 mt-0.5">{config.description}</div>
            )}
          </div>
          <div className={booleanKeys.has(config.configKey) ? '' : 'w-80'}>
            <ConfigField
              config={config}
              value={currentValue}
              canEdit={canEdit}
              onChange={(val) => setEdited({ ...edited, [config.configKey]: val })}
            />
          </div>
        </div>
      </div>
    );
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

      <div className="flex gap-2 mb-4 flex-wrap">
        {categories.map((cat) => {
          const Icon = categoryIcons[cat] || Settings;
          const label = categoryLabels[cat] || cat;
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
              <Icon className="w-4 h-4" /> {label}
            </button>
          );
        })}
      </div>

      {/* Main config section */}
      <div className="bg-white rounded-xl border border-slate-200 divide-y divide-slate-100">
        {aiOther.map(renderConfigRow)}
        {aiOther.length === 0 && aiFeatures.length === 0 && (
          <div className="text-center py-8 text-slate-500">No settings in this category.</div>
        )}
      </div>

    </div>
  );
}
