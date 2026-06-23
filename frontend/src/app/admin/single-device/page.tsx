"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/useAuth";
import {
  tenantAdminApi,
  singleDeviceAdminApi,
  type Tenant,
  type AccountType,
  type SingleDeviceConfigEntry,
} from "@/lib/adminApi";

const ACCOUNT_TYPES: AccountType[] = ["SCHOOL_ADMIN", "TEACHER", "STUDENT"];

const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  SCHOOL_ADMIN: "School Admin",
  TEACHER: "Teacher",
  STUDENT: "Student",
};

export default function SingleDeviceConfigPage() {
  const router = useRouter();
  const { accountType, isAuthenticated } = useAuth();

  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [selectedTenantId, setSelectedTenantId] = useState<string>("");
  const [globalEnabled, setGlobalEnabled] = useState(false);
  const [tenantConfigs, setTenantConfigs] = useState<SingleDeviceConfigEntry[]>([]);
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

  // Load tenant list on mount
  useEffect(() => {
    setLoading(true);
    tenantAdminApi
      .list()
      .then((t) => {
        setTenants(t);
        if (t.length > 0) setSelectedTenantId(t[0].id);
      })
      .catch(() => setError("Failed to load tenants"))
      .finally(() => setLoading(false));
  }, []);

  // Load config when selected tenant changes
  useEffect(() => {
    if (!selectedTenantId) return;
    setLoading(true);
    setError(null);
    setSuccessMsg(null);
    singleDeviceAdminApi
      .getConfig(selectedTenantId)
      .then((cfg) => {
        setGlobalEnabled(cfg.globalEnabled);
        // Ensure all three role types are represented in the list
        const merged: SingleDeviceConfigEntry[] = ACCOUNT_TYPES.map((at) => {
          const existing = cfg.tenantConfigs.find((c) => c.accountType === at);
          return existing ?? { accountType: at, enabled: false };
        });
        // Also carry the tenant-level (null accountType) entry
        const tenantLevel = cfg.tenantConfigs.find((c) => c.accountType === null);
        setTenantConfigs(
          tenantLevel ? [{ accountType: null, enabled: tenantLevel.enabled }, ...merged] : merged
        );
      })
      .catch(() => setError("Failed to load single-device config"))
      .finally(() => setLoading(false));
  }, [selectedTenantId]);

  function toggleEntry(accountTypeKey: AccountType | null) {
    setTenantConfigs((prev) =>
      prev.map((entry) =>
        entry.accountType === accountTypeKey
          ? { ...entry, enabled: !entry.enabled }
          : entry
      )
    );
  }

  async function handleSave() {
    if (!selectedTenantId) return;
    setSaving(true);
    setError(null);
    setSuccessMsg(null);
    try {
      await singleDeviceAdminApi.updateConfig(selectedTenantId, {
        globalEnabled,
        tenantConfigs,
      });
      setSuccessMsg("Single-device config saved successfully.");
    } catch {
      setError("Failed to save single-device config.");
    } finally {
      setSaving(false);
    }
  }

  if (accountType !== "SYSTEM_ADMIN") return null;

  const selectedTenant = tenants.find((t) => t.id === selectedTenantId);

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">
        Single-Device Login Configuration
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

      {/* Global toggle */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm px-6 py-4 mb-6 flex items-center justify-between">
        <div>
          <p className="text-sm font-semibold text-gray-900">
            Global single-device enforcement
          </p>
          <p className="text-xs text-gray-500 mt-0.5">
            When enabled, applies to all tenants and roles unless overridden below.
          </p>
        </div>
        <button
          type="button"
          role="switch"
          aria-checked={globalEnabled}
          onClick={() => setGlobalEnabled((v) => !v)}
          className={[
            "relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2",
            globalEnabled ? "bg-indigo-600" : "bg-gray-200",
          ].join(" ")}
        >
          <span
            className={[
              "pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow ring-0 transition-transform",
              globalEnabled ? "translate-x-5" : "translate-x-0",
            ].join(" ")}
          />
        </button>
      </div>

      {/* Tenant selector + per-role config */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200 flex flex-wrap gap-4 items-center justify-between">
          <div className="flex items-center gap-3">
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
              className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-900 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              {tenants.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} ({t.code})
                </option>
              ))}
            </select>
          </div>

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
          <p className="p-6 text-sm text-gray-400">Loading config…</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 border-b border-gray-100">
                <th className="px-6 py-3">Role / Level</th>
                <th className="px-6 py-3">Tenant</th>
                <th className="px-6 py-3 text-center">Enabled</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {tenantConfigs.map((entry) => {
                const label =
                  entry.accountType === null
                    ? "All roles (tenant-level)"
                    : ACCOUNT_TYPE_LABELS[entry.accountType];
                return (
                  <tr key={entry.accountType ?? "__tenant__"} className="hover:bg-gray-50">
                    <td className="px-6 py-3 font-medium text-gray-900">
                      {label}
                    </td>
                    <td className="px-6 py-3 text-gray-500">
                      {selectedTenant?.name ?? "—"}
                    </td>
                    <td className="px-6 py-3 text-center">
                      <button
                        type="button"
                        role="switch"
                        aria-checked={entry.enabled}
                        aria-label={`Toggle single-device for ${label}`}
                        onClick={() => toggleEntry(entry.accountType)}
                        className={[
                          "relative inline-flex h-5 w-10 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1",
                          entry.enabled ? "bg-indigo-600" : "bg-gray-200",
                        ].join(" ")}
                      >
                        <span
                          className={[
                            "pointer-events-none inline-block h-4 w-4 rounded-full bg-white shadow ring-0 transition-transform",
                            entry.enabled ? "translate-x-5" : "translate-x-0",
                          ].join(" ")}
                        />
                      </button>
                    </td>
                  </tr>
                );
              })}
              {tenantConfigs.length === 0 && (
                <tr>
                  <td
                    colSpan={3}
                    className="px-6 py-8 text-center text-sm text-gray-400"
                  >
                    No configuration available for this tenant.
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
