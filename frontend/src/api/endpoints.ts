import api from './client';

export const authApi = {
  login: (email: string, password: string) =>
    api.post('/auth/login', { email, password }),
  me: () => api.get('/auth/me'),
  changePassword: (currentPassword: string, newPassword: string) =>
    api.post('/auth/change-password', { currentPassword, newPassword }),
};

export const dashboardApi = {
  overview: () => api.get('/dashboard/overview'),
  health: () => api.get('/dashboard/health'),
};

export const tenantApi = {
  list: () => api.get('/tenants'),
  listActive: () => api.get('/tenants/active'),
  get: (id: string) => api.get(`/tenants/${id}`),
  create: (data: any) => api.post('/tenants', data),
  update: (id: string, data: any) => api.put(`/tenants/${id}`, data),
  deactivate: (id: string) => api.post(`/tenants/${id}/deactivate`),
  reactivate: (id: string) => api.post(`/tenants/${id}/reactivate`),
  toggleModule: (tenantId: string, moduleName: string, enabled: boolean) =>
    api.post(`/tenants/${tenantId}/modules/${moduleName}/toggle`, { enabled }),
  onboard: (data: any) => api.post('/tenants/onboard', data),
  supportAccess: (tenantId: string) => api.post(`/tenants/${tenantId}/support-access`),
};

export const userApi = {
  list: () => api.get('/users'),
  get: (id: string) => api.get(`/users/${id}`),
  create: (data: any) => api.post('/users', data),
  update: (id: string, data: any) => api.put(`/users/${id}`, data),
  deactivate: (id: string) => api.post(`/users/${id}/deactivate`),
  resetPassword: (id: string) => api.post(`/users/${id}/reset-password`),
};

export const configApi = {
  list: () => api.get('/config'),
  byCategory: (cat: string) => api.get(`/config/category/${cat}`),
  update: (data: Record<string, string>) => api.put('/config', data),
};

export const auditApi = {
  list: (page = 0, size = 50) => api.get('/audit', { params: { page, size } }),
  byAction: (action: string, page = 0, size = 50) =>
    api.get(`/audit/action/${action}`, { params: { page, size } }),
  byUser: (userId: string, page = 0, size = 50) =>
    api.get(`/audit/user/${userId}`, { params: { page, size } }),
};

export const roleApi = {
  list: () => api.get('/roles'),
};

export const planApi = {
  list: () => api.get('/plans'),
  byModule: (module: string) => api.get(`/plans/${module}`),
};
