import api from './client';

export interface ReleaseSummary {
  VERSION: string;
  PARENT_VERSION: string | null;
  STATUS: string;
  TITLE: string;
  ROW_COUNT: number;
  PUBLISHED_AT: string | null;
  PUBLISHED_BY: string | null;
  CREATED_AT: string;
  CREATED_BY: string;
}

export interface TenantPin {
  TENANT_ID: string;
  SCHEMA_NAME: string;
  IS_ACTIVE: number;
  SYSTEM_METADATA_VERSION: string | null;
  SYSTEM_METADATA_PINNED_AT: string | null;
  SYSTEM_METADATA_PINNED_BY: string | null;
  SYSTEM_METADATA_STATUS: string | null;
}

export interface DriftResult {
  tenantId: string;
  schema: string;
  pinnedVersion: string | null;
  manifestRowCount?: number;
  tenantRowCount?: number;
  netDelta?: number;
  warning?: string;
}

export const releasesApi = {
  list: () => api.get<ReleaseSummary[]>('/releases/list'),
  tenants: () => api.get<TenantPin[]>('/releases/tenants'),
  drift: (tenantId: string) =>
    api.get<DriftResult>(`/releases/drift?tenantId=${encodeURIComponent(tenantId)}`),
  getRelease: (version: string) =>
    api.get<Record<string, unknown>>(`/releases/version/${encodeURIComponent(version)}`),
};
