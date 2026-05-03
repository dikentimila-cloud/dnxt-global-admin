import { useEffect, useState } from 'react';
import { GitBranch, Building2, RefreshCw, AlertTriangle, CheckCircle2, Clock } from 'lucide-react';
import { releasesApi, ReleaseSummary, TenantPin, DriftResult } from '../api/releases';

type Tab = 'releases' | 'tenants';

export default function ReleasesPage() {
  const [tab, setTab] = useState<Tab>('releases');
  const [releases, setReleases] = useState<ReleaseSummary[]>([]);
  const [tenants, setTenants] = useState<TenantPin[]>([]);
  const [drift, setDrift] = useState<Record<string, DriftResult>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = () => {
    setLoading(true); setError(null);
    Promise.all([releasesApi.list(), releasesApi.tenants()])
      .then(([rRes, tRes]) => {
        setReleases(rRes.data || []);
        setTenants(tRes.data || []);
      })
      .catch(e => setError(e.response?.data?.error || e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { refresh(); }, []);

  const fetchDrift = (tenantId: string) => {
    releasesApi.drift(tenantId)
      .then(res => setDrift(d => ({ ...d, [tenantId]: res.data })))
      .catch(e => setDrift(d => ({ ...d, [tenantId]: { tenantId, schema: '', pinnedVersion: null, warning: e.message } })));
  };

  const tenantsOnVersion = (version: string) =>
    tenants.filter(t => t.SYSTEM_METADATA_VERSION === version).length;

  const statusBadge = (status: string | null) => {
    const colors: Record<string, string> = {
      UP_TO_DATE: 'bg-emerald-100 text-emerald-700 border-emerald-300',
      OUTDATED: 'bg-amber-100 text-amber-700 border-amber-300',
      DRIFT: 'bg-red-100 text-red-700 border-red-300',
      FAILED: 'bg-red-100 text-red-700 border-red-300',
      APPLYING: 'bg-blue-100 text-blue-700 border-blue-300',
      UNKNOWN: 'bg-slate-100 text-slate-600 border-slate-300',
    };
    const cls = colors[status || 'UNKNOWN'] || colors.UNKNOWN;
    return <span className={`px-2 py-0.5 text-xs font-medium rounded border ${cls}`}>{status || 'UNKNOWN'}</span>;
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <GitBranch className="w-5 h-5 text-slate-600" />
          <h2 className="text-xl font-bold text-slate-900">System Metadata Releases</h2>
          <span className="px-2 py-0.5 text-xs font-medium rounded bg-blue-100 text-blue-700 border border-blue-300">
            Phase 1 — read-only
          </span>
        </div>
        <button onClick={refresh} disabled={loading}
          className="flex items-center gap-2 px-3 py-1.5 text-sm border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50">
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      <div className="border-b border-slate-200 mb-4 flex gap-1">
        {(['releases', 'tenants'] as Tab[]).map(t => (
          <button key={t} onClick={() => setTab(t)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
              tab === t ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}>
            {t === 'releases' ? `Releases (${releases.length})` : `Tenants (${tenants.length})`}
          </button>
        ))}
      </div>

      {error && (
        <div className="flex items-center gap-2 p-3 mb-4 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          <AlertTriangle className="w-4 h-4 shrink-0" />
          {error}
        </div>
      )}

      {tab === 'releases' && (
        <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr className="text-left">
                <th className="px-3 py-2 font-medium text-slate-700">Version</th>
                <th className="px-3 py-2 font-medium text-slate-700">Status</th>
                <th className="px-3 py-2 font-medium text-slate-700">Title</th>
                <th className="px-3 py-2 font-medium text-slate-700 text-right">Operations</th>
                <th className="px-3 py-2 font-medium text-slate-700 text-right">Tenants on this</th>
                <th className="px-3 py-2 font-medium text-slate-700">Published</th>
              </tr>
            </thead>
            <tbody>
              {releases.map(r => (
                <tr key={r.VERSION} className="border-b border-slate-100 hover:bg-slate-50">
                  <td className="px-3 py-2 font-mono text-xs">{r.VERSION}</td>
                  <td className="px-3 py-2">{statusBadge(r.STATUS)}</td>
                  <td className="px-3 py-2 text-slate-700">{r.TITLE}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{r.ROW_COUNT.toLocaleString()}</td>
                  <td className="px-3 py-2 text-right tabular-nums font-medium">{tenantsOnVersion(r.VERSION)}</td>
                  <td className="px-3 py-2 text-slate-500 text-xs">
                    {r.PUBLISHED_AT ? new Date(r.PUBLISHED_AT).toLocaleString() : '—'}
                  </td>
                </tr>
              ))}
              {!releases.length && !loading && (
                <tr><td colSpan={6} className="px-3 py-6 text-center text-slate-500">No releases yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'tenants' && (
        <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr className="text-left">
                <th className="px-3 py-2 font-medium text-slate-700">Tenant</th>
                <th className="px-3 py-2 font-medium text-slate-700">Pinned Version</th>
                <th className="px-3 py-2 font-medium text-slate-700">Status</th>
                <th className="px-3 py-2 font-medium text-slate-700">Pinned At</th>
                <th className="px-3 py-2 font-medium text-slate-700 text-right">Drift</th>
              </tr>
            </thead>
            <tbody>
              {tenants.map(t => {
                const d = drift[t.TENANT_ID];
                return (
                  <tr key={t.TENANT_ID} className="border-b border-slate-100 hover:bg-slate-50">
                    <td className="px-3 py-2">
                      <div className="flex items-center gap-2">
                        <Building2 className="w-4 h-4 text-slate-400" />
                        <span className="font-medium">{t.TENANT_ID}</span>
                        <span className="text-xs text-slate-500">({t.SCHEMA_NAME})</span>
                      </div>
                    </td>
                    <td className="px-3 py-2 font-mono text-xs">{t.SYSTEM_METADATA_VERSION || '—'}</td>
                    <td className="px-3 py-2">{statusBadge(t.SYSTEM_METADATA_STATUS)}</td>
                    <td className="px-3 py-2 text-slate-500 text-xs">
                      {t.SYSTEM_METADATA_PINNED_AT ? (
                        <span className="flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          {new Date(t.SYSTEM_METADATA_PINNED_AT).toLocaleString()}
                        </span>
                      ) : '—'}
                    </td>
                    <td className="px-3 py-2 text-right">
                      {d ? (
                        d.netDelta === 0 ? (
                          <span className="inline-flex items-center gap-1 text-xs text-emerald-700">
                            <CheckCircle2 className="w-3.5 h-3.5" /> in sync
                          </span>
                        ) : d.netDelta != null ? (
                          <span className="inline-flex items-center gap-1 text-xs text-amber-700">
                            <AlertTriangle className="w-3.5 h-3.5" /> Δ {d.netDelta > 0 ? '+' : ''}{d.netDelta}
                          </span>
                        ) : (
                          <span className="text-xs text-slate-500">{d.warning || 'n/a'}</span>
                        )
                      ) : (
                        <button onClick={() => fetchDrift(t.TENANT_ID)}
                          className="text-xs text-blue-600 hover:underline">Check</button>
                      )}
                    </td>
                  </tr>
                );
              })}
              {!tenants.length && !loading && (
                <tr><td colSpan={5} className="px-3 py-6 text-center text-slate-500">No tenants found.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      <p className="text-xs text-slate-500 mt-4">
        Phase 1 read-only. Stage / Publish / Apply land in Phase 2 + 3. See{' '}
        <span className="font-mono">docs/system-metadata-release-design.md</span> for the design.
      </p>
    </div>
  );
}
