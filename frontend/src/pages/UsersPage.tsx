import { useEffect, useState } from 'react';
import { userApi, roleApi } from '../api/endpoints';
import { useStore } from '../store/useStore';
import type { AdminUser } from '../types';
import { Users, Plus, Shield, KeyRound, Pencil } from 'lucide-react';

export default function UsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [roles, setRoles] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [editUser, setEditUser] = useState<AdminUser | null>(null);
  const hasPermission = useStore((s) => s.hasPermission);

  const [form, setForm] = useState({ email: '', firstName: '', lastName: '', roleId: '' });

  useEffect(() => {
    Promise.all([userApi.list(), roleApi.list()])
      .then(([usersRes, rolesRes]) => {
        setUsers(usersRes.data.data);
        setRoles(rolesRes.data.data);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await userApi.create(form);
      setUsers([...users, res.data.data]);
      setShowCreate(false);
      setForm({ email: '', firstName: '', lastName: '', roleId: '' });
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create user');
    }
  };

  const handleEditRole = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editUser) return;
    const newRoleId = (e.target as any).roleId.value;
    try {
      const res = await userApi.update(editUser.userId, {
        email: editUser.email,
        firstName: editUser.firstName,
        lastName: editUser.lastName,
        roleId: newRoleId,
      });
      setUsers(users.map((u) => u.userId === editUser.userId ? res.data.data : u));
      setEditUser(null);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update role');
    }
  };

  const handleDeactivate = async (userId: string) => {
    if (!confirm('Deactivate this user?')) return;
    try {
      await userApi.deactivate(userId);
      setUsers(users.map((u) => u.userId === userId ? { ...u, isActive: false } : u));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to deactivate user');
    }
  };

  const handleResetPassword = async (userId: string) => {
    if (!confirm('Reset this user\'s password? They will receive a new temporary password.')) return;
    try {
      await userApi.resetPassword(userId);
      alert('Password reset. User will receive new credentials.');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to reset password');
    }
  };

  if (loading) return <div className="text-center text-slate-500 py-12">Loading...</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-bold text-slate-900">Admin Users</h2>
        {hasPermission('USER_CREATE') && (
          <button onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-sm font-medium hover:bg-slate-800 transition-colors">
            <Plus className="w-4 h-4" /> New User
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200">
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">User</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Email</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Role</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Status</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Last Login</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.userId} className="border-b border-slate-100 hover:bg-slate-50 transition-colors">
                <td className="px-4 py-3">
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-8 bg-slate-200 rounded-full flex items-center justify-center text-xs font-medium text-slate-600">
                      {u.firstName?.charAt(0)}{u.lastName?.charAt(0)}
                    </div>
                    <span className="font-medium text-slate-800">{u.firstName} {u.lastName}</span>
                  </div>
                </td>
                <td className="px-4 py-3 text-sm text-slate-600">{u.email}</td>
                <td className="px-4 py-3">
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-100 rounded text-xs font-medium text-slate-600">
                    <Shield className="w-3 h-3" /> {u.role?.roleLabel || '-'}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                    u.isActive ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                  }`}>
                    {u.isActive ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm text-slate-500">
                  {u.lastLogin ? new Date(u.lastLogin).toLocaleDateString() : 'Never'}
                </td>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-1">
                    {hasPermission('USER_EDIT') && (
                      <button onClick={() => setEditUser(u)}
                        className="p-1.5 text-slate-400 hover:text-blue-600 transition-colors" title="Edit Role">
                        <Pencil className="w-4 h-4" />
                      </button>
                    )}
                    {hasPermission('USER_EDIT') && (
                      <button onClick={() => handleResetPassword(u.userId)}
                        className="p-1.5 text-slate-400 hover:text-amber-600 transition-colors" title="Reset Password">
                        <KeyRound className="w-4 h-4" />
                      </button>
                    )}
                    {hasPermission('USER_DEACTIVATE') && u.isActive && (
                      <button onClick={() => handleDeactivate(u.userId)}
                        className="p-1.5 text-slate-400 hover:text-red-600 transition-colors" title="Deactivate">
                        <Users className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Create User Modal */}
      {showCreate && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6">
            <h3 className="text-lg font-bold text-slate-900 mb-1">Create Admin User</h3>
            <p className="text-sm text-slate-500 mb-4">Only @dnxtsolutions.com emails are allowed.</p>
            <form onSubmit={handleCreate} className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Email *</label>
                <input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })}
                  placeholder="name@dnxtsolutions.com"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">First Name *</label>
                  <input type="text" required value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Last Name *</label>
                  <input type="text" required value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Role *</label>
                <select required value={form.roleId} onChange={(e) => setForm({ ...form, roleId: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                  <option value="">Select role...</option>
                  {roles.map((r: any) => (
                    <option key={r.roleId} value={r.roleId}>{r.roleLabel}</option>
                  ))}
                </select>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowCreate(false)}
                  className="flex-1 py-2 border border-slate-300 rounded-lg text-sm text-slate-700 hover:bg-slate-50 transition-colors">Cancel</button>
                <button type="submit"
                  className="flex-1 py-2 bg-slate-900 text-white rounded-lg text-sm font-medium hover:bg-slate-800 transition-colors">Create</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Role Modal */}
      {editUser && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6">
            <h3 className="text-lg font-bold text-slate-900 mb-1">Edit User</h3>
            <p className="text-sm text-slate-500 mb-4">{editUser.firstName} {editUser.lastName} ({editUser.email})</p>
            <form onSubmit={handleEditRole} className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Role</label>
                <select name="roleId" defaultValue={editUser.roleId || ''}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                  {roles.map((r: any) => (
                    <option key={r.roleId} value={r.roleId}>{r.roleLabel} — {r.description || r.roleName}</option>
                  ))}
                </select>
              </div>
              <div className="bg-slate-50 rounded-lg p-3 text-xs text-slate-600 space-y-1">
                <div><strong>Super Admin:</strong> Full access — tenants, users, config, audit</div>
                <div><strong>Admin:</strong> Manage tenants & modules, view dashboard</div>
                <div><strong>Viewer:</strong> Read-only access to everything</div>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setEditUser(null)}
                  className="flex-1 py-2 border border-slate-300 rounded-lg text-sm text-slate-700 hover:bg-slate-50 transition-colors">Cancel</button>
                <button type="submit"
                  className="flex-1 py-2 bg-slate-900 text-white rounded-lg text-sm font-medium hover:bg-slate-800 transition-colors">Save</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
