"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/useAuth";
import {
  tenantAdminApi,
  permissionAdminApi,
  rolePermissionAdminApi,
  type Tenant,
  type Permission,
  type AccountType,
} from "@/lib/adminApi";

const ACCOUNT_TYPES: AccountType[] = ["SCHOOL_ADMIN", "TEACHER", "STUDENT"];

const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  SCHOOL_ADMIN: "School Admin",
  TEACHER: "Teacher",
  STUDENT: "Student",
};

export default function RolePermissionPage() {
  const router = useRouter();
  const { accountType, isAuthenticated } = useAuth();

  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [selectedTenantId, setSelectedTenantId] = useState<string>("");
  const [selectedAccountType, setSelectedAccountType] =
    useState<AccountType>("STUDENT");
  const [allPermissions, setAllPermissions] = useState<Permission[]>([]);
  const [checkedIds, setCheckedIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Guard: SYSTEM_ADMIN only
  useEffect(() => {
    if (!isAuthenticated() || accountType !== "SYSTEM_ADMIN") {
      router.replace("/login");
    }
  }, [accountType, isAuthenticated, router]);

  // Load tenants + all permissions on mount
  useEffect(() => {
    setLoading(true);
    Promise.all([tenantAdminApi.list(), permissionAdminApi.listAll()])
      .then(([t, p]) => {
        setTenants(t);
        setAllPermissions(p);
        if (t.length > 0) setSelectedTenantId(t[0].id);
      })
      .catch(() => setError("Failed to load data"))
      .finally(() => setLoading(false));
  }, []);

  // Load role permissions when tenant or role changes
  useEffect(() => {
    if (!selectedTenantId) return;
    setLoading(true);
    setError(null);
    setSuccessMsg(null);
    rolePermissionAdminApi
      .get(selectedTenantId, selectedAccountType)
      .then((perms) => {
        const enabled = new Set(
          perms.filter((p) => p.enabled).map((p) => p.permissionId)
        );
        setCheckedIds(enabled);
      })
      .catch(() => setError("Failed to load role permissions"))
      .finally(() => setLoading(false));
  }, [selectedTenantId, selectedAccountType]);

  function togglePermission(id: string) {
    setCheckedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function handleSave() {
    if (!selectedTenantId) return;
    setSaving(true);
    setError(null);
    setSuccessMsg(null);
    try {
      await rolePermissionAdminApi.update(
        selectedTenantId,
        selectedAccountType,
        Array.from(checkedIds)
      );
      setSuccessMsg("Role permissions saved successfully.");
    } catch {
      setError("Failed to save role permissions.");
    } finally {
      setSaving(false);
    }
  }

  if (accountType !== "SYSTEM_ADMIN") return null;

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">
        Role Permission Management
      </h1>

      {error && (
        <div className="mb-4 rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}
      {successMsg && (
        <div className="mb-4 rounded-md bg-green-50 border border-green-200 px-4 py-3 text-sm text-green-700">
          {successMsg}
        </div>
      )}

      {/* Selectors row */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm px-6 py-4 mb-6 flex flex-wrap gap-6 items-end">
        <div className="flex flex-col gap-1">
          <label
            htmlFor="tenant-select"
            className="text-xs font-semibold uppercase tracking-wide text-gray-500"
          >
            Tenant
          </label>
          <select
            id="tenant-select"
            value={selectedTenantId}
            onChange={(e) => setSelectedTenantId(e.target.value)}
            className="rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            {tenants.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name} ({t.code})
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1">
          <label
            htmlFor="role-select"
            className="text-xs font-semibold uppercase tracking-wide text-gray-500"
          >
            Role
          </label>
          <div className="flex gap-2" role="group" aria-label="Role selector">
            {ACCOUNT_TYPES.map((at) => (
              <button
                key={at}
                type="button"
                onClick={() => setSelectedAccountType(at)}
                className={[
                  "px-4 py-2 rounded-md text-sm font-medium border transition-colors",
                  selectedAccountType === at
                    ? "bg-indigo-600 text-white border-indigo-600"
                    : "bg-white text-gray-700 border-gray-300 hover:bg-gray-50",
                ].join(" ")}
                aria-pressed={selectedAccountType === at}
              >
                {ACCOUNT_TYPE_LABELS[at]}
              </button>
            ))}
          </div>
        </div>

        <button
          type="button"
          onClick={handleSave}
          disabled={saving || !selectedTenantId}
          className="ml-auto px-4 py-2 rounded-md bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {saving ? "Saving…" : "Save Changes"}
        </button>
      </div>

      {/* Permissions table */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        <div className="px-6 py-3 bg-gray-50 border-b border-gray-200 text-xs font-semibold uppercase tracking-wide text-gray-500">
          Permissions for{" "}
          {ACCOUNT_TYPE_LABELS[selectedAccountType]} in{" "}
          {tenants.find((t) => t.id === selectedTenantId)?.name ?? "—"}
        </div>

        {loading ? (
          <p className="p-6 text-sm text-gray-400">Loading permissions…</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs font-semibold uppercase tracking-wide text-gray-500 border-b border-gray-100">
                <th className="px-6 py-3 w-10">
                  <span className="sr-only">Enabled</span>
                </th>
                <th className="px-6 py-3">Permission</th>
                <th className="px-6 py-3">Code</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {allPermissions.map((p) => (
                <tr
                  key={p.id}
                  className="hover:bg-gray-50 cursor-pointer"
                  onClick={() => togglePermission(p.id)}
                >
                  <td className="px-6 py-3">
                    <input
                      type="checkbox"
                      readOnly
                      checked={checkedIds.has(p.id)}
                      className="h-4 w-4 rounded border-gray-300 text-indigo-600 cursor-pointer"
                      aria-label={`Toggle ${p.name}`}
                    />
                  </td>
                  <td className="px-6 py-3 text-gray-900 font-medium">
                    {p.name}
                    {p.description && (
                      <span className="block text-xs text-gray-400 font-normal">
                        {p.description}
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-3 text-gray-500 font-mono text-xs">
                    {p.code}
                  </td>
                </tr>
              ))}
              {allPermissions.length === 0 && (
                <tr>
                  <td
                    colSpan={3}
                    className="px-6 py-8 text-center text-sm text-gray-400"
                  >
                    No permissions defined.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
