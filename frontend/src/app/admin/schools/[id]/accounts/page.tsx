"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/lib/useAuth";
import {
  accountApi,
  schoolApi,
  type AccountRow,
  type AccountType,
  type AccountStatus,
} from "@/lib/schoolApi";
import AccountFormDialog from "@/components/school/AccountFormDialog";

const PAGE_SIZE = 20;

const ACCOUNT_TYPE_LABEL: Record<AccountType, string> = {
  SCHOOL_ADMIN: "管理员",
  TEACHER: "教师",
  STUDENT: "学生",
};

const STATUS_LABEL: Record<AccountStatus, string> = {
  ACTIVE: "启用",
  DISABLED: "停用",
};

export default function AccountManagePage() {
  const router = useRouter();
  const params = useParams();
  const { isAuthenticated, accountId: myAccountId, accountType } = useAuth();
  const schoolId = typeof params.id === "string" ? params.id : "";

  const [schoolName, setSchoolName] = useState("");
  const [rows, setRows] = useState<AccountRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [filterName, setFilterName] = useState("");
  const [filterLoginName, setFilterLoginName] = useState("");
  const [filterType, setFilterType] = useState<AccountType | "">("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);
  const [selected, setSelected] = useState<Set<string>>(new Set());

  // 账户弹窗
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<AccountRow | null>(null);

  // Guard: SCHOOL_ADMIN or SYSTEM_ADMIN
  useEffect(() => {
    if (!isAuthenticated() || (accountType !== "SCHOOL_ADMIN" && accountType !== "SYSTEM_ADMIN")) {
      router.replace("/login");
    }
  }, [isAuthenticated, accountType, router]);

  // 加载学校名称
  useEffect(() => {
    if (!schoolId) return;
    let cancelled = false;
    schoolApi.get(schoolId).then((d) => { if (!cancelled) setSchoolName(d.name); }).catch(() => {});
    return () => { cancelled = true; };
  }, [schoolId]);

  const load = useCallback(
    async (p: number, name: string, loginName: string, type: AccountType | "") => {
      if (!schoolId) return;
      setLoading(true);
      setError(null);
      try {
        const result = await accountApi.list(schoolId, {
          page: p,
          size: PAGE_SIZE,
          ...(name ? { name } : {}),
          ...(loginName ? { loginName } : {}),
          ...(type ? { accountType: type } : {}),
        });
        setRows(result.content);
        setTotal(result.totalElements);
      } catch {
        setError("加载账户列表失败，请稍后重试。");
      } finally {
        setLoading(false);
      }
    },
    [schoolId]
  );

  useEffect(() => {
    load(page, filterName, filterLoginName, filterType);
  }, [page, load, filterName, filterLoginName, filterType]);

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setPage(0);
    load(0, filterName, filterLoginName, filterType);
  }

  function handleReset() {
    setFilterName("");
    setFilterLoginName("");
    setFilterType("");
    setPage(0);
    load(0, "", "", "");
  }

  async function handleToggleStatus(row: AccountRow) {
    const next: AccountStatus = row.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    const label = next === "ACTIVE" ? "启用" : "停用";
    if (!confirm(`确认${label}账户「${row.name}」？`)) return;
    try {
      await accountApi.patchStatus(schoolId, row.id, next);
      setStatusMsg(`${label}成功`);
      load(page, filterName, filterLoginName, filterType);
    } catch {
      setError(`${label}失败，请稍后重试。`);
    }
  }

  async function handleDelete(row: AccountRow) {
    if (row.id === myAccountId) { setError("不可删除当前登录账户。"); return; }
    if (!confirm(`确认删除账户「${row.name}（${row.loginName}）」？此操作不可恢复。`)) return;
    try {
      await accountApi.remove(schoolId, row.id);
      setStatusMsg("删除成功");
      setSelected((prev) => { const s = new Set(prev); s.delete(row.id); return s; });
      load(page, filterName, filterLoginName, filterType);
    } catch {
      setError("删除失败，请稍后重试。");
    }
  }

  async function handleBatchDelete() {
    const ids = Array.from(selected).filter((id) => id !== myAccountId);
    if (ids.length === 0) { setError("没有可删除的账户（已自动排除当前账户）。"); return; }
    if (!confirm(`确认删除选中的 ${ids.length} 个账户？此操作不可恢复。`)) return;
    try {
      const result = await accountApi.batchRemove(schoolId, ids);
      setStatusMsg(`已删除 ${result.deletedCount} 个账户`);
      setSelected(new Set());
      load(page, filterName, filterLoginName, filterType);
    } catch {
      setError("批量删除失败，请稍后重试。");
    }
  }

  async function handleBatchDisable() {
    const ids = Array.from(selected);
    if (ids.length === 0) return;
    if (!confirm(`确认停用选中的 ${ids.length} 个账户？`)) return;
    try {
      await accountApi.batchPatchStatus(schoolId, ids, "DISABLED");
      setStatusMsg(`已停用 ${ids.length} 个账户`);
      setSelected(new Set());
      load(page, filterName, filterLoginName, filterType);
    } catch {
      setError("批量停用失败，请稍后重试。");
    }
  }

  function toggleSelect(id: string) {
    setSelected((prev) => {
      const s = new Set(prev);
      if (s.has(id)) s.delete(id); else s.add(id);
      return s;
    });
  }

  function toggleSelectAll() {
    if (selected.size === rows.length) {
      setSelected(new Set());
    } else {
      setSelected(new Set(rows.map((r) => r.id)));
    }
  }

  async function handleResetPassword(row: AccountRow) {
    if (!confirm(`将为「${row.name}」重置为系统默认密码，确认继续？`)) return;
    try {
      await accountApi.resetPassword(schoolId, row.id, null);
      setStatusMsg("密码已重置");
    } catch {
      setError("重置密码失败，请稍后重试。");
    }
  }

  function handleOpenCreate() {
    setEditTarget(null);
    setDialogOpen(true);
  }

  function handleOpenEdit(row: AccountRow) {
    setEditTarget(row);
    setDialogOpen(true);
  }

  function handleDialogSuccess() {
    setDialogOpen(false);
    load(page, filterName, filterLoginName, filterType);
  }

  const totalPages = Math.ceil(total / PAGE_SIZE);
  const allSelected = rows.length > 0 && selected.size === rows.length;

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      {/* 页头 */}
      <div className="flex items-center gap-3 mb-1">
        <Link href="/admin/schools" className="text-sm text-indigo-600 hover:text-indigo-800">
          ← 学校列表
        </Link>
      </div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">账户管理</h1>
          {schoolName && <p className="mt-0.5 text-sm text-gray-500">{schoolName}</p>}
        </div>
        <div className="flex gap-2">
          <Link
            href={`/admin/schools/${schoolId}/accounts/batch-import`}
            className="inline-flex items-center px-4 py-2 rounded-md border border-gray-300 text-gray-700 text-sm font-medium hover:bg-gray-100 transition-colors"
          >
            批量导入
          </Link>
          <button
            type="button"
            onClick={handleOpenCreate}
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 transition-colors"
          >
            <span aria-hidden="true">+</span> 新建账户
          </button>
        </div>
      </div>

      {/* 消息条 */}
      {error && (
        <div role="alert" className="mb-4 rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {error}
          <button
            type="button"
            onClick={() => setError(null)}
            className="ml-2 text-red-500 hover:text-red-700 font-medium"
            aria-label="关闭错误提示"
          >
            ×
          </button>
        </div>
      )}
      {statusMsg && (
        <div role="status" className="mb-4 rounded-md bg-green-50 border border-green-200 px-4 py-3 text-sm text-green-700">
          {statusMsg}
          <button
            type="button"
            onClick={() => setStatusMsg(null)}
            className="ml-2 text-green-500 hover:text-green-700 font-medium"
            aria-label="关闭提示"
          >
            ×
          </button>
        </div>
      )}

      {/* 搜索栏 */}
      <form onSubmit={handleSearch} className="mb-4 flex flex-wrap gap-3">
        <input
          type="text"
          value={filterLoginName}
          onChange={(e) => setFilterLoginName(e.target.value)}
          placeholder="登录名"
          aria-label="按登录名搜索"
          className="w-36 rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
        <input
          type="text"
          value={filterName}
          onChange={(e) => setFilterName(e.target.value)}
          placeholder="姓名"
          aria-label="按姓名搜索"
          className="w-36 rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
        <select
          value={filterType}
          onChange={(e) => setFilterType(e.target.value as AccountType | "")}
          aria-label="账户类型筛选"
          className="w-32 rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        >
          <option value="">全部类型</option>
          <option value="SCHOOL_ADMIN">管理员</option>
          <option value="TEACHER">教师</option>
          <option value="STUDENT">学生</option>
        </select>
        <button
          type="submit"
          className="px-4 py-1.5 rounded-md bg-gray-800 text-white text-sm font-medium hover:bg-gray-700 transition-colors"
        >
          搜索
        </button>
        <button
          type="button"
          onClick={handleReset}
          className="px-4 py-1.5 rounded-md border border-gray-300 text-gray-600 text-sm hover:bg-gray-100 transition-colors"
        >
          重置
        </button>
      </form>

      {/* 批量操作栏 */}
      {selected.size > 0 && (
        <div className="mb-3 flex items-center gap-3 px-4 py-2 bg-indigo-50 border border-indigo-200 rounded-md text-sm text-indigo-700">
          <span>已选 {selected.size} 条</span>
          <button
            type="button"
            onClick={handleBatchDisable}
            className="px-3 py-1 rounded border border-indigo-300 hover:bg-indigo-100 transition-colors"
          >
            批量停用
          </button>
          <button
            type="button"
            onClick={handleBatchDelete}
            className="px-3 py-1 rounded border border-red-300 text-red-600 hover:bg-red-50 transition-colors"
          >
            批量删除
          </button>
        </div>
      )}

      {/* 表格 */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        {loading ? (
          <p className="p-6 text-sm text-gray-400">加载中…</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
                <th className="px-4 py-3 w-10">
                  <input
                    type="checkbox"
                    checked={allSelected}
                    onChange={toggleSelectAll}
                    aria-label="全选"
                    className="h-4 w-4 rounded border-gray-300 text-indigo-600"
                  />
                </th>
                <th className="px-4 py-3">登录名</th>
                <th className="px-4 py-3">姓名</th>
                <th className="px-4 py-3">类型</th>
                <th className="px-4 py-3">第二身份</th>
                <th className="px-4 py-3">状态</th>
                <th className="px-4 py-3">创建时间</th>
                <th className="px-4 py-3 text-right">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {rows.map((row) => (
                <tr key={row.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <input
                      type="checkbox"
                      checked={selected.has(row.id)}
                      onChange={() => toggleSelect(row.id)}
                      aria-label={`选择 ${row.name}`}
                      className="h-4 w-4 rounded border-gray-300 text-indigo-600"
                    />
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-gray-600">{row.loginName}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">{row.name}</td>
                  <td className="px-4 py-3 text-gray-600">
                    {ACCOUNT_TYPE_LABEL[row.accountType]}
                  </td>
                  <td className="px-4 py-3 text-gray-500 text-xs">
                    {row.secondaryRole ?? "—"}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={[
                        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
                        row.status === "ACTIVE"
                          ? "bg-green-100 text-green-700"
                          : "bg-gray-100 text-gray-500",
                      ].join(" ")}
                    >
                      {STATUS_LABEL[row.status]}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500 text-xs">{row.createdAt.slice(0, 10)}</td>
                  <td className="px-4 py-3 text-right">
                    <div className="inline-flex gap-2">
                      <button
                        type="button"
                        onClick={() => handleOpenEdit(row)}
                        className="text-xs font-medium text-indigo-600 hover:text-indigo-800"
                      >
                        编辑
                      </button>
                      <button
                        type="button"
                        onClick={() => handleResetPassword(row)}
                        className="text-xs font-medium text-amber-600 hover:text-amber-800"
                      >
                        重置密码
                      </button>
                      <button
                        type="button"
                        onClick={() => handleToggleStatus(row)}
                        className={[
                          "text-xs font-medium",
                          row.status === "ACTIVE"
                            ? "text-red-600 hover:text-red-800"
                            : "text-green-600 hover:text-green-800",
                        ].join(" ")}
                      >
                        {row.status === "ACTIVE" ? "停用" : "启用"}
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDelete(row)}
                        disabled={row.id === myAccountId}
                        className="text-xs font-medium text-red-600 hover:text-red-800 disabled:opacity-30 disabled:cursor-not-allowed"
                      >
                        删除
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {rows.length === 0 && !loading && (
                <tr>
                  <td colSpan={8} className="px-6 py-8 text-center text-sm text-gray-400">
                    暂无账户数据
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* 分页 */}
      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
          <span>共 {total} 条</span>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              className="px-3 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-100 transition-colors"
            >
              上一页
            </button>
            <span className="px-3 py-1">{page + 1} / {totalPages}</span>
            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="px-3 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-100 transition-colors"
            >
              下一页
            </button>
          </div>
        </div>
      )}

      {/* 账户表单弹窗 */}
      {dialogOpen && (
        <AccountFormDialog
          schoolId={schoolId}
          account={editTarget}
          onSuccess={handleDialogSuccess}
          onClose={() => setDialogOpen(false)}
        />
      )}
    </div>
  );
}
