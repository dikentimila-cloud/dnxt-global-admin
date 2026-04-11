export interface Tenant {
  tenantId: string;
  tenantName: string;
  tenantSlug: string;
  domain: string | null;
  industry: string | null;
  logoUrl: string | null;
  primaryContactName: string;
  primaryContactEmail: string;
  phone: string | null;
  address: string | null;
  status: string;
  licenseType: string | null;
  licenseExpiry: string | null;
  maxUsers: number;
  notes: string | null;
  createdDate: string;
  modifiedDate: string;
  isActive: boolean;
}

export interface TenantModule {
  moduleId: string;
  tenantId: string;
  moduleName: string;
  isEnabled: boolean;
  licensedUsers: number;
  activatedDate: string | null;
  expiryDate: string | null;
}

export interface AdminUser {
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roleId: string;
  role: { roleId: string; roleName: string; roleLabel: string } | null;
  isActive: boolean;
  mustChangePassword: boolean;
  lastLogin: string | null;
  createdDate: string;
}

export interface AuditEntry {
  logId: string;
  userId: string;
  username: string;
  action: string;
  targetType: string;
  targetId: string;
  targetName: string;
  details: string;
  ipAddress: string;
  createdDate: string;
}

export interface PlatformConfig {
  configKey: string;
  configValue: string;
  category: string;
  description: string;
  isSecret: boolean;
}

export interface DashboardOverview {
  totalTenants: number;
  activeTenants: number;
  inactiveTenants: number;
  totalLicenses: number;
  operationsLicenses: number;
  reviewerLicenses: number;
  publisherLicenses: number;
  edmsLicenses: number;
  plannerLicenses: number;
  totalAdminUsers: number;
  activeAdminUsers: number;
  totalAuditEntries: number;
}
