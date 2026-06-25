"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/useAuth";
import {
  tenantAdminApi,
  permissionAdminApi,
  type Tenant,
  type Permission,
  type TenantPermission,
} from "@/lib/adminApi";

export default function TenantPermissionPage() {
  const router = useRouter();
  const { accountType, isAuthenticated } = useAuth();

  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [selectedTenantId, setSelectedTenantId] = useState<string | null>(null);
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

  // Load per-tenant permissions when selection changes
  useEffect(() => {
    if (!selectedTenantId) return;
    setLoading(true);
    setError(null);
    permissionAdminApi
      .getTenantPermissions(selectedTenantId)
      .then((perms: TenantPermission[]) => {
        const enabled = new Set(
          perms.filter((p) => p.enabled).map((p) => p.permissionId)
        );
        setCheckedIds(enabled);
      })
      .catch(() => setError("Failed to load tenant permissions"))
      .finally(() => setLoading(false));
  }, [selectedTenantId]);

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
      await permissionAdminApi.updateTenantPermissions(
        selectedTenantId,
        Array.from(checkedIds)
      );
      setSuccessMsg("Permissions saved successfully.");
    } catch {
      setError("Failed to save permissions.");
    } finally {
      setSaving(false);
    }
  }

  if (accountType !== "SYSTEM_ADMIN") return null;

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">
        Tenant Permission Management
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

      <div className="flex gap-6">
        {/* Left panel — tenant list */}
        <aside className="w-64 shrink-0 bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div className="px-4 py-3 bg-gray-100 border-b border-gray-200 text-xs font-semibold uppercase tracking-wide text-gray-500">
            Tenants
          </div>
          {loading && tenants.length === 0 ? (
            <p className="p-4 text-sm text-gray-400">Loading…</p>
          ) : (
            <ul>
              {tenants.map((t) => (
                <li key={t.id}>
                  <button
                    type="button"
                    onClick={() => setSelectedTenantId(t.id)}
                    className={[
                      "w-full text-left px-4 py-3 text-sm border-b border-gray-100 transition-colors",
                      selectedTenantId === t.id
                        ? "bg-indigo-50 text-indigo-700 font-medium"
                        : "text-gray-700 hover:bg-gray-50",
                    ].join(" ")}
                  >
                    <span className="block truncate">{t.name}</span>
                    <span className="block text-xs text-gray-400">{t.code}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </aside>

        {/* Right panel — permissions */}
        <section className="flex-1 bg-white rounded-lg border border-gray-200 shadow-sm">
          <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-gray-700">
              {selectedTenantId
                ? `Permissions — ${tenants.find((t) => t.id === selectedTenantId)?.name ?? ""}`
                : "Select a tenant"}
            </h2>
            <button
              type="button"
              onClick={handleSave}
              disabled={saving || !selectedTenantId}
              className="px-4 py-2 rounded-md bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {saving ? "Saving…" : "Save"}
            </button>
          </div>

          {loading ? (
            <p className="p-6 text-sm text-gray-400">Loading permissions…</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
                  <th className="px-6 py-3 w-10">
                    <span className="sr-only">Enabled</span>
                  </th>
                  <th className="px-6 py-3">Permission</th>
                  <th className="px-6 py-3">Code</th>
                  <th className="px-6 py-3">Status</th>
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
                    <td className="px-6 py-3">
                      <span
                        className={[
                          "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
                          p.status === "ACTIVE"
                            ? "bg-green-100 text-green-700"
                            : "bg-gray-100 text-gray-500",
                        ].join(" ")}
                      >
                        {p.status}
                      </span>
                    </td>
                  </tr>
                ))}
                {allPermissions.length === 0 && (
                  <tr>
                    <td
                      colSpan={4}
                      className="px-6 py-8 text-center text-sm text-gray-400"
                    >
                      No permissions defined.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </div>
  );
}
