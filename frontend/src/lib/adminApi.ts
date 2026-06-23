import axios from "axios";

// ── Authenticated admin HTTP client ──────────────────────────────────────────
// Reads token from the same "caa_token" JSON blob that useAuth writes.

const TOKEN_KEY = "caa_token";

function getBearerToken(): string | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem(TOKEN_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as { token?: string };
    return parsed.token ?? null;
  } catch {
    return null;
  }
}

const adminHttp = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
  timeout: 30000,
});

adminHttp.interceptors.request.use((config) => {
  const token = getBearerToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

adminHttp.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && typeof window !== "undefined") {
      window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);

// ── Types ─────────────────────────────────────────────────────────────────────

export type AccountType = "SCHOOL_ADMIN" | "TEACHER" | "STUDENT";

export interface Tenant {
  id: string;
  code: string;
  name: string;
  type: "ADMIN" | "SCHOOL" | "WECHAT";
  status: "ACTIVE" | "INACTIVE";
  defaultLoginType: "PASSWORD" | "WECHAT" | "SSO";
}

export interface Permission {
  id: string;
  code: string;
  name: string;
  description: string | null;
  status: "ACTIVE" | "INACTIVE";
}

export interface TenantPermission {
  permissionId: string;
  permissionCode: string;
  permissionName: string;
  enabled: boolean;
}

export interface RolePermission {
  permissionId: string;
  permissionCode: string;
  permissionName: string;
  enabled: boolean;
}

export interface SingleDeviceConfigEntry {
  accountType: AccountType | null; // null = tenant-level
  enabled: boolean;
}

export interface SingleDeviceConfig {
  globalEnabled: boolean;
  tenantConfigs: SingleDeviceConfigEntry[];
}

// ── Tenant endpoints ──────────────────────────────────────────────────────────

export const tenantAdminApi = {
  list: (): Promise<Tenant[]> =>
    adminHttp.get<{ data: Tenant[] }>("/admin/tenants").then((r) => r.data.data),

  get: (tenantId: string): Promise<Tenant> =>
    adminHttp
      .get<{ data: Tenant }>(`/admin/tenants/${tenantId}`)
      .then((r) => r.data.data),
};

// ── Permission endpoints ──────────────────────────────────────────────────────

export const permissionAdminApi = {
  /** List all system permissions */
  listAll: (): Promise<Permission[]> =>
    adminHttp
      .get<{ data: Permission[] }>("/admin/permissions")
      .then((r) => r.data.data),

  /** Get per-tenant permission toggles */
  getTenantPermissions: (tenantId: string): Promise<TenantPermission[]> =>
    adminHttp
      .get<{ data: TenantPermission[] }>(`/admin/tenants/${tenantId}/permissions`)
      .then((r) => r.data.data),

  /** Replace per-tenant permission list */
  updateTenantPermissions: (
    tenantId: string,
    permissionIds: string[]
  ): Promise<void> =>
    adminHttp
      .put(`/admin/tenants/${tenantId}/permissions`, { permissionIds })
      .then(() => undefined),
};

// ── Role-permission endpoints ─────────────────────────────────────────────────

export const rolePermissionAdminApi = {
  /** Get role permissions for a given tenant + accountType */
  get: (tenantId: string, accountType: AccountType): Promise<RolePermission[]> =>
    adminHttp
      .get<{ data: RolePermission[] }>(
        `/admin/tenants/${tenantId}/role-permissions/${accountType}`
      )
      .then((r) => r.data.data),

  /** Replace role permissions for a given tenant + accountType */
  update: (
    tenantId: string,
    accountType: AccountType,
    permissionIds: string[]
  ): Promise<void> =>
    adminHttp
      .put(`/admin/tenants/${tenantId}/role-permissions/${accountType}`, {
        permissionIds,
      })
      .then(() => undefined),
};

// ── Single-device config endpoints ───────────────────────────────────────────

export const singleDeviceAdminApi = {
  /** Get global + per-tenant single-device config */
  getConfig: (tenantId: string): Promise<SingleDeviceConfig> =>
    adminHttp
      .get<{ data: SingleDeviceConfig }>(
        `/admin/tenants/${tenantId}/single-device-config`
      )
      .then((r) => r.data.data),

  /** Update single-device config for a tenant */
  updateConfig: (
    tenantId: string,
    config: SingleDeviceConfig
  ): Promise<void> =>
    adminHttp
      .put(`/admin/tenants/${tenantId}/single-device-config`, config)
      .then(() => undefined),
};
