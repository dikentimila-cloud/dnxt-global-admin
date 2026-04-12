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

export const aiConfigApi = {
  state: () => api.get('/aigateway/admin/state'),
  providers: () => api.get('/aigateway/admin/providers'),
  createProvider: (data: any) => api.post('/aigateway/admin/providers', data),
  updateProvider: (id: string, data: any) => api.put(`/aigateway/admin/providers/${id}`, data),
  deleteProvider: (id: string) => api.delete(`/aigateway/admin/providers/${id}`),
  models: (providerId?: string) => api.get('/aigateway/admin/models', { params: providerId ? { providerId } : {} }),
  createModel: (data: any) => api.post('/aigateway/admin/models', data),
  updateModel: (id: string, data: any) => api.put(`/aigateway/admin/models/${id}`, data),
  deleteModel: (id: string) => api.delete(`/aigateway/admin/models/${id}`),
  credentials: () => api.get('/aigateway/admin/credentials'),
  saveCredential: (data: any) => api.post('/aigateway/admin/credentials', data),
  deleteCredential: (providerId: string) => api.delete(`/aigateway/admin/credentials/${providerId}`),
  tenant: () => api.get('/aigateway/admin/tenant'),
  saveTenant: (data: any) => api.put('/aigateway/admin/tenant', data),
  quickSetup: () => api.post('/aigateway/admin/quick-setup'),
  active: () => api.get('/aigateway/admin/active'),
  test: (providerId?: string) => api.post('/aigateway/admin/test', providerId ? { providerId } : {}),
  usageSummary: (days = 30) => api.get('/aigateway/admin/usage/summary', { params: { days } }),
  usageRecent: (page = 0, size = 50) => api.get('/aigateway/admin/usage/recent', { params: { page, size } }),
  usageTenant: (tenantId: string, days = 30) => api.get(`/aigateway/admin/usage/tenant/${tenantId}`, { params: { days } }),
};

export const planApi = {
  list: () => api.get('/plans'),
  byModule: (module: string) => api.get(`/plans/${module}`),
  create: (data: any) => api.post('/plans', data),
  update: (planId: string, data: any) => api.put(`/plans/${planId}`, data),
  delete: (planId: string) => api.delete(`/plans/${planId}`),
};
