import { useEffect, useState } from 'react';
import { aiConfigApi } from '../api/endpoints';
import { useStore } from '../store/useStore';
import {
  Brain, Zap, Plus, Trash2, Save, TestTube, ChevronDown, ChevronUp,
  Check, X, Loader2, Key, Server, Cpu, Shield, BarChart3, DollarSign, Activity
} from 'lucide-react';

interface Provider {
  providerId: string;
  providerName: string;
  displayName: string;
  providerType: string;
  baseEndpoint: string;
  apiVersion: string;
  isActive: string;
  supportsStreaming: string;
  supportsFunctions: string;
  supportsVision: string;
}

interface Model {
  modelId: string;
  providerId: string;
  modelName: string;
  modelDisplayName: string;
  modelType: string;
  deploymentName: string;
  maxInputTokens: number;
  maxOutputTokens: number;
  isActive: string;
}

interface Credential {
  credentialId: string;
  providerId: string;
  apiKey: string | null;
  endpointOverride: string;
  resourceName: string;
  isActive: string;
}

interface TenantConfig {
  configId: string;
  aiEnabled: string;
  defaultProviderId: string | null;
  defaultModelId: string | null;
  maxRequestsPerMinute: number | null;
  maxCostPerDay: number | null;
  maxCostPerMonth: number | null;
  costAlertThreshold: number | null;
}

const PROVIDER_TYPES = [
  { value: 'ANTHROPIC', label: 'Anthropic', icon: '🟣' },
  { value: 'OPENAI', label: 'OpenAI', icon: '🟢' },
  { value: 'AZURE_OPENAI', label: 'Azure OpenAI', icon: '🔵' },
  { value: 'OLLAMA', label: 'Ollama', icon: '🟠' },
];

export default function AiConfigPage() {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [models, setModels] = useState<Model[]>([]);
  const [credentials, setCredentials] = useState<Credential[]>([]);
  const [tenantConfig, setTenantConfig] = useState<TenantConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [expandedProvider, setExpandedProvider] = useState<string | null>(null);
  const [testResult, setTestResult] = useState<{ providerId: string; ok: boolean; reply?: string; error?: string } | null>(null);
  const [testing, setTesting] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [showAddProvider, setShowAddProvider] = useState(false);
  const [showAddModel, setShowAddModel] = useState<string | null>(null);
  const [apiKeyInputs, setApiKeyInputs] = useState<Record<string, string>>({});
  const [activeTab, setActiveTab] = useState<'providers' | 'usage'>('providers');
  const [usageSummary, setUsageSummary] = useState<any>(null);
  const [usageDays, setUsageDays] = useState(30);
  const [usageLoading, setUsageLoading] = useState(false);

  const hasPermission = useStore((s) => s.hasPermission);
  const canEdit = hasPermission('CONFIG_EDIT');

  const loadState = async () => {
    try {
      const res = await aiConfigApi.state();
      const d = res.data.data;
      setProviders(d.providers || []);
      setModels(d.models || []);
      setCredentials(d.credentials || []);
      setTenantConfig(d.tenantConfig || null);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  };

  const loadUsage = async (days: number) => {
    setUsageLoading(true);
    try {
      const res = await aiConfigApi.usageSummary(days);
      setUsageSummary(res.data.data);
    } catch (e) { console.error(e); }
    finally { setUsageLoading(false); }
  };

  useEffect(() => { loadState(); }, []);
  useEffect(() => { if (activeTab === 'usage') loadUsage(usageDays); }, [activeTab, usageDays]);

  const handleQuickSetup = async () => {
    setSaving(true);
    try {
      await aiConfigApi.quickSetup();
      await loadState();
    } catch (e: any) { alert(e.response?.data?.message || 'Quick setup failed'); }
    finally { setSaving(false); }
  };

  const handleToggleAi = async () => {
    if (!tenantConfig) return;
    setSaving(true);
    try {
      const res = await aiConfigApi.saveTenant({
        ...tenantConfig,
        aiEnabled: tenantConfig.aiEnabled === 'Y' ? 'N' : 'Y',
      });
      setTenantConfig(res.data.data);
    } catch (e: any) { alert(e.response?.data?.message || 'Failed'); }
    finally { setSaving(false); }
  };

  const handleSetDefault = async (providerId: string, modelId?: string) => {
    if (!tenantConfig) return;
    setSaving(true);
    try {
      const res = await aiConfigApi.saveTenant({
        ...tenantConfig,
        defaultProviderId: providerId,
        defaultModelId: modelId || tenantConfig.defaultModelId,
      });
      setTenantConfig(res.data.data);
    } catch (e: any) { alert(e.response?.data?.message || 'Failed'); }
    finally { setSaving(false); }
  };

  const handleTest = async (providerId?: string) => {
    const id = providerId || 'active';
    setTesting(id);
    setTestResult(null);
    try {
      const res = await aiConfigApi.test(providerId);
      setTestResult({ providerId: id, ok: true, reply: res.data.data.reply });
    } catch (e: any) {
      setTestResult({ providerId: id, ok: false, error: e.response?.data?.message || 'Test failed' });
    } finally { setTesting(null); }
  };

  const handleSaveApiKey = async (providerId: string) => {
    const key = apiKeyInputs[providerId];
    if (!key) return;
    setSaving(true);
    try {
      await aiConfigApi.saveCredential({ providerId, apiKey: key });
      setApiKeyInputs(prev => ({ ...prev, [providerId]: '' }));
      await loadState();
    } catch (e: any) { alert(e.response?.data?.message || 'Failed to save API key'); }
    finally { setSaving(false); }
  };

  const handleDeleteProvider = async (providerId: string) => {
    if (!confirm('Delete this provider and all its models/credentials?')) return;
    try {
      await aiConfigApi.deleteProvider(providerId);
      await loadState();
    } catch (e: any) { alert(e.response?.data?.message || 'Failed'); }
  };

  const handleDeleteModel = async (modelId: string) => {
    if (!confirm('Delete this model?')) return;
    try {
      await aiConfigApi.deleteModel(modelId);
      await loadState();
    } catch (e: any) { alert(e.response?.data?.message || 'Failed'); }
  };

  const handleAddProvider = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    setSaving(true);
    try {
      await aiConfigApi.createProvider({
        providerName: form.get('providerName'),
        displayName: form.get('displayName'),
        providerType: form.get('providerType'),
        baseEndpoint: form.get('baseEndpoint'),
        apiVersion: form.get('apiVersion'),
        isActive: 'Y', supportsStreaming: 'Y', supportsFunctions: 'Y', supportsVision: 'N',
      });
      setShowAddProvider(false);
      await loadState();
    } catch (e: any) { alert(e.response?.data?.message || 'Failed'); }
    finally { setSaving(false); }
  };

  const handleAddModel = async (e: React.FormEvent<HTMLFormElement>, providerId: string) => {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    setSaving(true);
    try {
      await aiConfigApi.createModel({
        providerId,
        modelName: form.get('modelName'),
        modelDisplayName: form.get('modelDisplayName'),
        modelType: 'CHAT',
        maxInputTokens: parseInt(form.get('maxInputTokens') as string) || 128000,
        maxOutputTokens: parseInt(form.get('maxOutputTokens') as string) || 4096,
        isActive: 'Y',
      });
      setShowAddModel(null);
      await loadState();
    } catch (e: any) { alert(e.response?.data?.message || 'Failed'); }
    finally { setSaving(false); }
  };

  if (loading) return <div className="text-center text-slate-500 py-12">Loading AI configuration...</div>;

  const isDefault = (providerId: string) => tenantConfig?.defaultProviderId === providerId;
  const isDefaultModel = (modelId: string) => tenantConfig?.defaultModelId === modelId;
  const getCred = (providerId: string) => credentials.find(c => c.providerId === providerId);
  const getModels = (providerId: string) => models.filter(m => m.providerId === providerId);
  const getTypeInfo = (type: string) => PROVIDER_TYPES.find(t => t.value === type) || { value: type, label: type, icon: '⚙️' };

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
            <Brain className="w-6 h-6 text-purple-600" /> AI Configuration
          </h2>
          <p className="text-sm text-slate-500 mt-1">Configure AI providers, models, and API keys</p>
        </div>
        <div className="flex items-center gap-3">
          {tenantConfig && (
            <button onClick={handleToggleAi} disabled={saving}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                tenantConfig.aiEnabled === 'Y'
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100'
                  : 'bg-slate-100 text-slate-500 border border-slate-200 hover:bg-slate-200'
              }`}>
              {tenantConfig.aiEnabled === 'Y' ? <><Zap className="w-4 h-4" /> AI Enabled</> : <><X className="w-4 h-4" /> AI Disabled</>}
            </button>
          )}
          {providers.length === 0 && (
            <button onClick={handleQuickSetup} disabled={saving}
              className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 disabled:opacity-50">
              <Zap className="w-4 h-4" /> Quick Setup
            </button>
          )}
          {canEdit && (
            <button onClick={() => setShowAddProvider(true)}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700">
              <Plus className="w-4 h-4" /> Add Provider
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        <button onClick={() => setActiveTab('providers')}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            activeTab === 'providers' ? 'bg-slate-900 text-white' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'}`}>
          <Server className="w-4 h-4" /> Providers & Models
        </button>
        <button onClick={() => setActiveTab('usage')}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            activeTab === 'usage' ? 'bg-slate-900 text-white' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'}`}>
          <BarChart3 className="w-4 h-4" /> Usage & Costs
        </button>
      </div>

      {/* Usage Tab */}
      {activeTab === 'usage' && (
        <div>
          <div className="flex items-center gap-3 mb-4">
            <select value={usageDays} onChange={e => setUsageDays(parseInt(e.target.value))}
              className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white">
              <option value={7}>Last 7 days</option>
              <option value={14}>Last 14 days</option>
              <option value={30}>Last 30 days</option>
              <option value={90}>Last 90 days</option>
            </select>
          </div>

          {usageLoading ? (
            <div className="text-center py-12 text-slate-500"><Loader2 className="w-6 h-6 animate-spin mx-auto" /></div>
          ) : usageSummary ? (
            <div>
              {/* Totals */}
              <div className="grid grid-cols-3 gap-4 mb-6">
                <div className="bg-white rounded-xl border border-slate-200 p-5">
                  <div className="flex items-center gap-2 text-slate-500 text-xs uppercase mb-2"><Activity className="w-4 h-4" /> Total Requests</div>
                  <div className="text-2xl font-bold text-slate-900">{Number(usageSummary.totalRequests).toLocaleString()}</div>
                </div>
                <div className="bg-white rounded-xl border border-slate-200 p-5">
                  <div className="flex items-center gap-2 text-slate-500 text-xs uppercase mb-2"><Cpu className="w-4 h-4" /> Total Tokens</div>
                  <div className="text-2xl font-bold text-slate-900">{Number(usageSummary.totalTokens).toLocaleString()}</div>
                </div>
                <div className="bg-white rounded-xl border border-slate-200 p-5">
                  <div className="flex items-center gap-2 text-slate-500 text-xs uppercase mb-2"><DollarSign className="w-4 h-4" /> Total Cost</div>
                  <div className="text-2xl font-bold text-slate-900">${Number(usageSummary.totalCost).toFixed(4)}</div>
                </div>
              </div>

              {/* By Tenant */}
              <div className="mb-6">
                <h3 className="text-sm font-semibold text-slate-700 mb-3">Cost by Tenant</h3>
                <div className="bg-white rounded-xl border border-slate-200 divide-y divide-slate-100">
                  {usageSummary.byTenant && usageSummary.byTenant.length > 0 ? usageSummary.byTenant.map((t: any, i: number) => (
                    <div key={i} className="px-6 py-3 flex items-center justify-between">
                      <div>
                        <div className="text-sm font-medium text-slate-800">{t.tenantName || t.tenantId || 'Unknown'}</div>
                        <div className="text-xs text-slate-500">{Number(t.requests).toLocaleString()} requests &middot; {Number(t.tokens).toLocaleString()} tokens</div>
                      </div>
                      <div className="text-sm font-semibold text-slate-900">${Number(t.cost).toFixed(4)}</div>
                    </div>
                  )) : (
                    <div className="px-6 py-8 text-center text-slate-400 text-sm">No usage data yet. AI calls will appear here once made.</div>
                  )}
                </div>
              </div>

              {/* By Model */}
              <div>
                <h3 className="text-sm font-semibold text-slate-700 mb-3">Cost by Model</h3>
                <div className="bg-white rounded-xl border border-slate-200 divide-y divide-slate-100">
                  {usageSummary.byModel && usageSummary.byModel.length > 0 ? usageSummary.byModel.map((m: any, i: number) => (
                    <div key={i} className="px-6 py-3 flex items-center justify-between">
                      <div>
                        <div className="text-sm font-medium text-slate-800">{m.provider} / {m.model}</div>
                        <div className="text-xs text-slate-500">{Number(m.requests).toLocaleString()} requests &middot; {Number(m.tokens).toLocaleString()} tokens</div>
                      </div>
                      <div className="text-sm font-semibold text-slate-900">${Number(m.cost).toFixed(4)}</div>
                    </div>
                  )) : (
                    <div className="px-6 py-8 text-center text-slate-400 text-sm">No model usage data yet.</div>
                  )}
                </div>
              </div>
            </div>
          ) : (
            <div className="text-center py-12 text-slate-400">No usage data available.</div>
          )}
        </div>
      )}

      {/* Providers Tab */}
      {activeTab === 'providers' && <>
      {/* Test Active Config */}
      {tenantConfig?.aiEnabled === 'Y' && tenantConfig.defaultProviderId && (
        <div className="mb-6 bg-purple-50 border border-purple-200 rounded-xl p-4 flex items-center justify-between">
          <div className="text-sm text-purple-700">
            <span className="font-medium">Active:</span>{' '}
            {providers.find(p => p.providerId === tenantConfig.defaultProviderId)?.displayName || 'Unknown'}{' / '}
            {models.find(m => m.modelId === tenantConfig.defaultModelId)?.modelDisplayName || 'Auto-select'}
          </div>
          <button onClick={() => handleTest()} disabled={testing !== null}
            className="flex items-center gap-2 px-3 py-1.5 bg-purple-600 text-white rounded-lg text-xs font-medium hover:bg-purple-700 disabled:opacity-50">
            {testing === 'active' ? <Loader2 className="w-3 h-3 animate-spin" /> : <TestTube className="w-3 h-3" />} Test Connection
          </button>
        </div>
      )}

      {testResult && testResult.providerId === 'active' && (
        <div className={`mb-4 p-3 rounded-lg text-sm ${testResult.ok ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-red-50 text-red-700 border border-red-200'}`}>
          {testResult.ok ? <><Check className="w-4 h-4 inline mr-1" /> Reply: {testResult.reply}</> : <><X className="w-4 h-4 inline mr-1" /> {testResult.error}</>}
        </div>
      )}

      {/* Add Provider Form */}
      {showAddProvider && (
        <div className="mb-6 bg-white rounded-xl border border-slate-200 p-6">
          <h3 className="text-sm font-semibold text-slate-700 mb-4">Add Provider</h3>
          <form onSubmit={handleAddProvider} className="grid grid-cols-2 gap-4">
            <input name="providerName" placeholder="Provider name (e.g. my-anthropic)" required className="px-3 py-2 border border-slate-300 rounded-lg text-sm" />
            <input name="displayName" placeholder="Display name (e.g. Anthropic Claude)" required className="px-3 py-2 border border-slate-300 rounded-lg text-sm" />
            <select name="providerType" required className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white">
              {PROVIDER_TYPES.map(t => <option key={t.value} value={t.value}>{t.icon} {t.label}</option>)}
            </select>
            <input name="baseEndpoint" placeholder="Base endpoint URL" className="px-3 py-2 border border-slate-300 rounded-lg text-sm" />
            <input name="apiVersion" placeholder="API version (optional)" className="px-3 py-2 border border-slate-300 rounded-lg text-sm" />
            <div className="flex gap-2">
              <button type="submit" disabled={saving} className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
                <Save className="w-4 h-4 inline mr-1" /> Save
              </button>
              <button type="button" onClick={() => setShowAddProvider(false)} className="px-4 py-2 bg-slate-100 text-slate-600 rounded-lg text-sm hover:bg-slate-200">Cancel</button>
            </div>
          </form>
        </div>
      )}

      {/* Providers List */}
      {providers.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-xl border border-slate-200">
          <Brain className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <p className="text-slate-500 text-sm">No AI providers configured.</p>
          <p className="text-slate-400 text-xs mt-1">Click Quick Setup to add Anthropic, Azure OpenAI, and Ollama with default models.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {providers.map(provider => {
            const expanded = expandedProvider === provider.providerId;
            const cred = getCred(provider.providerId);
            const providerModels = getModels(provider.providerId);
            const typeInfo = getTypeInfo(provider.providerType);
            const isDef = isDefault(provider.providerId);

            return (
              <div key={provider.providerId} className={`bg-white rounded-xl border ${isDef ? 'border-purple-300 ring-1 ring-purple-100' : 'border-slate-200'}`}>
                {/* Provider Header */}
                <div className="px-6 py-4 flex items-center justify-between cursor-pointer" onClick={() => setExpandedProvider(expanded ? null : provider.providerId)}>
                  <div className="flex items-center gap-3">
                    <span className="text-xl">{typeInfo.icon}</span>
                    <div>
                      <div className="text-sm font-semibold text-slate-800 flex items-center gap-2">
                        {provider.displayName || provider.providerName}
                        {isDef && <span className="text-[10px] bg-purple-100 text-purple-700 px-2 py-0.5 rounded-full font-medium">DEFAULT</span>}
                        {provider.isActive === 'Y'
                          ? <span className="text-[10px] bg-emerald-100 text-emerald-700 px-2 py-0.5 rounded-full">Active</span>
                          : <span className="text-[10px] bg-slate-100 text-slate-500 px-2 py-0.5 rounded-full">Inactive</span>}
                      </div>
                      <div className="text-xs text-slate-500 mt-0.5">
                        {typeInfo.label} &middot; {providerModels.length} model{providerModels.length !== 1 ? 's' : ''} &middot;{' '}
                        {cred?.apiKey ? <><Key className="w-3 h-3 inline" /> Key set</> : <span className="text-amber-600">No API key</span>}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {!isDef && canEdit && (
                      <button onClick={(e) => { e.stopPropagation(); handleSetDefault(provider.providerId); }}
                        className="text-xs px-3 py-1 bg-purple-50 text-purple-600 rounded-lg hover:bg-purple-100">Set as Default</button>
                    )}
                    <button onClick={(e) => { e.stopPropagation(); handleTest(provider.providerId); }} disabled={testing !== null}
                      className="text-xs px-3 py-1 bg-slate-50 text-slate-600 rounded-lg hover:bg-slate-100 disabled:opacity-50">
                      {testing === provider.providerId ? <Loader2 className="w-3 h-3 animate-spin inline" /> : <TestTube className="w-3 h-3 inline" />} Test
                    </button>
                    {canEdit && (
                      <button onClick={(e) => { e.stopPropagation(); handleDeleteProvider(provider.providerId); }}
                        className="text-xs px-2 py-1 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-lg">
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    )}
                    {expanded ? <ChevronUp className="w-4 h-4 text-slate-400" /> : <ChevronDown className="w-4 h-4 text-slate-400" />}
                  </div>
                </div>

                {/* Test result for this provider */}
                {testResult && testResult.providerId === provider.providerId && (
                  <div className={`mx-6 mb-3 p-2 rounded-lg text-xs ${testResult.ok ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
                    {testResult.ok ? <><Check className="w-3 h-3 inline mr-1" /> {testResult.reply}</> : <><X className="w-3 h-3 inline mr-1" /> {testResult.error}</>}
                  </div>
                )}

                {/* Expanded Details */}
                {expanded && (
                  <div className="px-6 pb-5 border-t border-slate-100 pt-4 space-y-4">
                    {/* API Key */}
                    <div className="flex items-center gap-3">
                      <Key className="w-4 h-4 text-slate-400" />
                      <span className="text-sm text-slate-600 w-20">API Key</span>
                      {cred?.apiKey ? (
                        <span className="text-sm text-emerald-600 font-mono">***ENCRYPTED***</span>
                      ) : (
                        <span className="text-sm text-amber-600">Not set</span>
                      )}
                      {canEdit && (
                        <div className="flex gap-2 ml-auto">
                          <input type="password" placeholder="Enter new API key..."
                            value={apiKeyInputs[provider.providerId] || ''}
                            onChange={e => setApiKeyInputs(prev => ({ ...prev, [provider.providerId]: e.target.value }))}
                            className="px-3 py-1 border border-slate-300 rounded-lg text-sm w-64" />
                          <button onClick={() => handleSaveApiKey(provider.providerId)} disabled={!apiKeyInputs[provider.providerId] || saving}
                            className="px-3 py-1 bg-blue-600 text-white rounded-lg text-xs font-medium hover:bg-blue-700 disabled:opacity-50">Save Key</button>
                        </div>
                      )}
                    </div>

                    {/* Endpoint */}
                    <div className="flex items-center gap-3">
                      <Server className="w-4 h-4 text-slate-400" />
                      <span className="text-sm text-slate-600 w-20">Endpoint</span>
                      <span className="text-sm text-slate-800 font-mono">{provider.baseEndpoint || 'Default'}</span>
                    </div>

                    {/* Models */}
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <Cpu className="w-4 h-4 text-slate-400" />
                          <span className="text-sm font-medium text-slate-700">Models</span>
                        </div>
                        {canEdit && (
                          <button onClick={() => setShowAddModel(showAddModel === provider.providerId ? null : provider.providerId)}
                            className="text-xs px-3 py-1 bg-slate-50 text-slate-600 rounded-lg hover:bg-slate-100">
                            <Plus className="w-3 h-3 inline mr-1" /> Add Model
                          </button>
                        )}
                      </div>

                      {showAddModel === provider.providerId && (
                        <form onSubmit={(e) => handleAddModel(e, provider.providerId)} className="mb-3 p-3 bg-slate-50 rounded-lg flex gap-3 items-end">
                          <div className="flex-1">
                            <label className="text-[10px] text-slate-500 uppercase">Model ID</label>
                            <input name="modelName" placeholder="e.g. claude-sonnet-4-6" required className="w-full px-2 py-1 border border-slate-300 rounded text-sm" />
                          </div>
                          <div className="flex-1">
                            <label className="text-[10px] text-slate-500 uppercase">Display Name</label>
                            <input name="modelDisplayName" placeholder="e.g. Claude Sonnet 4.6" required className="w-full px-2 py-1 border border-slate-300 rounded text-sm" />
                          </div>
                          <div className="w-28">
                            <label className="text-[10px] text-slate-500 uppercase">Max Input</label>
                            <input name="maxInputTokens" type="number" defaultValue={128000} className="w-full px-2 py-1 border border-slate-300 rounded text-sm" />
                          </div>
                          <div className="w-28">
                            <label className="text-[10px] text-slate-500 uppercase">Max Output</label>
                            <input name="maxOutputTokens" type="number" defaultValue={4096} className="w-full px-2 py-1 border border-slate-300 rounded text-sm" />
                          </div>
                          <button type="submit" disabled={saving} className="px-3 py-1 bg-blue-600 text-white rounded text-xs">Save</button>
                          <button type="button" onClick={() => setShowAddModel(null)} className="px-3 py-1 bg-slate-200 text-slate-600 rounded text-xs">Cancel</button>
                        </form>
                      )}

                      <div className="space-y-1">
                        {providerModels.map(model => (
                          <div key={model.modelId} className={`flex items-center justify-between px-3 py-2 rounded-lg ${isDefaultModel(model.modelId) ? 'bg-purple-50 border border-purple-200' : 'bg-slate-50'}`}>
                            <div className="flex items-center gap-3">
                              <span className={`text-sm font-medium ${isDefaultModel(model.modelId) ? 'text-purple-700' : 'text-slate-700'}`}>
                                {model.modelDisplayName || model.modelName}
                              </span>
                              <span className="text-[10px] font-mono text-slate-400">{model.modelName}</span>
                              {isDefaultModel(model.modelId) && <span className="text-[10px] bg-purple-100 text-purple-600 px-1.5 py-0.5 rounded">Default</span>}
                              <span className="text-[10px] text-slate-400">{(model.maxInputTokens / 1000).toFixed(0)}K in / {(model.maxOutputTokens / 1000).toFixed(0)}K out</span>
                            </div>
                            <div className="flex gap-1">
                              {isDef && !isDefaultModel(model.modelId) && canEdit && (
                                <button onClick={() => handleSetDefault(provider.providerId, model.modelId)}
                                  className="text-[10px] px-2 py-0.5 text-purple-600 hover:bg-purple-100 rounded">Set Default</button>
                              )}
                              {canEdit && (
                                <button onClick={() => handleDeleteModel(model.modelId)} className="text-red-400 hover:text-red-600 p-1 rounded hover:bg-red-50">
                                  <Trash2 className="w-3 h-3" />
                                </button>
                              )}
                            </div>
                          </div>
                        ))}
                        {providerModels.length === 0 && <div className="text-xs text-slate-400 py-2">No models configured.</div>}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
      </>}
    </div>
  );
}
