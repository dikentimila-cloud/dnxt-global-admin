import { useEffect, useState } from 'react';
import { auditApi } from '../api/endpoints';
import type { AuditEntry } from '../types';
import { Shield, ChevronLeft, ChevronRight } from 'lucide-react';

export default function AuditPage() {
  const [entries, setEntries] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchLogs = (p: number) => {
    setLoading(true);
    auditApi.list(p, 30)
      .then((res) => {
        setEntries(res.data.data.content);
        setTotalPages(res.data.data.totalPages);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchLogs(page); }, [page]);

  const actionColors: Record<string, string> = {
    LOGIN_SUCCESS: 'bg-green-100 text-green-700',
    LOGIN_FAILED: 'bg-red-100 text-red-700',
    LOGIN_REJECTED: 'bg-red-100 text-red-700',
    LOGIN_RATE_LIMITED: 'bg-amber-100 text-amber-700',
    TENANT_CREATED: 'bg-blue-100 text-blue-700',
    TENANT_UPDATED: 'bg-blue-100 text-blue-700',
    TENANT_DEACTIVATED: 'bg-red-100 text-red-700',
    TENANT_REACTIVATED: 'bg-green-100 text-green-700',
    TENANT_ONBOARDED: 'bg-purple-100 text-purple-700',
    MODULE_ENABLED: 'bg-green-100 text-green-700',
    MODULE_DISABLED: 'bg-amber-100 text-amber-700',
    USER_CREATED: 'bg-blue-100 text-blue-700',
    USER_DEACTIVATED: 'bg-red-100 text-red-700',
    PASSWORD_CHANGED: 'bg-amber-100 text-amber-700',
    PASSWORD_RESET: 'bg-amber-100 text-amber-700',
    CONFIG_UPDATED: 'bg-purple-100 text-purple-700',
  };

  return (
    <div>
      <div className="flex items-center gap-2 mb-6">
        <Shield className="w-5 h-5 text-slate-600" />
        <h2 className="text-xl font-bold text-slate-900">Audit Log</h2>
      </div>

      {loading ? (
        <div className="text-center text-slate-500 py-12">Loading audit log...</div>
      ) : (
        <>
          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200">
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Time</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Action</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Target</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Details</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">IP</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((e) => (
                  <tr key={e.logId} className="border-b border-slate-100 hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                      {new Date(e.createdDate).toLocaleString()}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${
                        actionColors[e.action] || 'bg-slate-100 text-slate-600'
                      }`}>
                        {e.action}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-600">
                      {e.targetName || e.targetType || '-'}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-500 max-w-xs truncate">
                      {e.details || '-'}
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-400 font-mono">{e.ipAddress}</td>
                  </tr>
                ))}
                {entries.length === 0 && (
                  <tr><td colSpan={5} className="text-center py-8 text-slate-500">No audit entries.</td></tr>
                )}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-4 mt-4">
              <button
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="flex items-center gap-1 px-3 py-1.5 text-sm border border-slate-300 rounded-lg hover:bg-slate-50 disabled:opacity-50 transition-colors"
              >
                <ChevronLeft className="w-4 h-4" /> Previous
              </button>
              <span className="text-sm text-slate-500">Page {page + 1} of {totalPages}</span>
              <button
                onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="flex items-center gap-1 px-3 py-1.5 text-sm border border-slate-300 rounded-lg hover:bg-slate-50 disabled:opacity-50 transition-colors"
              >
                Next <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
