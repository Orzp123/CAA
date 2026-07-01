"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/lib/useAuth";
import {
  schoolApi,
  type SchoolRow,
  type SchoolStatus,
} from "@/lib/schoolApi";

const PAGE_SIZE = 20;

export default function SchoolListPage() {
  const router = useRouter();
  const { accountType, isAuthenticated } = useAuth();

  const [rows, setRows] = useState<SchoolRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [searchName, setSearchName] = useState("");
  const [searchCode, setSearchCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);

  // Guard: SYSTEM_ADMIN only
  useEffect(() => {
    if (!isAuthenticated() || accountType !== "SYSTEM_ADMIN") {
      router.replace("/login");
    }
  }, [accountType, isAuthenticated, router]);

  const load = useCallback(
    async (p: number, name: string, code: string) => {
      setLoading(true);
      setError(null);
      try {
        const result = await schoolApi.list({
          page: p,
          size: PAGE_SIZE,
          ...(name ? { name } : {}),
          ...(code ? { code } : {}),
        });
        setRows(result.content);
        setTotal(result.totalElements);
      } catch {
        setError("加载学校列表失败，请稍后重试。");
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    if (accountType === "SYSTEM_ADMIN") {
      load(page, searchName, searchCode);
    }
  }, [accountType, page, load, searchName, searchCode]);

  async function handleToggleStatus(school: SchoolRow) {
    const next: SchoolStatus = school.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    const label = next === "ACTIVE" ? "启用" : "停用";
    if (!confirm(`确认${label}「${school.name}」？`)) return;
    try {
      await schoolApi.patchStatus(school.id, next);
      setStatusMsg(`${label}成功`);
      load(page, searchName, searchCode);
    } catch {
      setError(`${label}失败，请稍后重试。`);
    }
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setPage(0);
    load(0, searchName, searchCode);
  }

  const totalPages = Math.ceil(total / PAGE_SIZE);

  if (accountType !== "SYSTEM_ADMIN") return null;

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      {/* 页头 */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">学校管理</h1>
        <Link
          href="/admin/schools/new"
          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 transition-colors"
        >
          <span aria-hidden="true">+</span> 新建学校
        </Link>
      </div>

      {/* 消息条 */}
      {error && (
        <div role="alert" className="mb-4 rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}
      {statusMsg && (
        <div role="status" className="mb-4 rounded-md bg-green-50 border border-green-200 px-4 py-3 text-sm text-green-700">
          {statusMsg}
        </div>
      )}

      {/* 搜索栏 */}
      <form onSubmit={handleSearch} className="mb-4 flex gap-3">
        <input
          type="text"
          value={searchName}
          onChange={(e) => setSearchName(e.target.value)}
          placeholder="学校名称"
          aria-label="按名称搜索"
          className="w-48 rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-900 placeholder-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
        <input
          type="text"
          value={searchCode}
          onChange={(e) => setSearchCode(e.target.value)}
          placeholder="学校代码"
          aria-label="按代码搜索"
          className="w-40 rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-900 placeholder-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
        <button
          type="submit"
          className="px-4 py-1.5 rounded-md bg-gray-800 text-white text-sm font-medium hover:bg-gray-700 transition-colors"
        >
          搜索
        </button>
        <button
          type="button"
          onClick={() => { setSearchName(""); setSearchCode(""); setPage(0); load(0, "", ""); }}
          className="px-4 py-1.5 rounded-md border border-gray-300 text-gray-600 text-sm hover:bg-gray-100 transition-colors"
        >
          重置
        </button>
      </form>

      {/* 表格 */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        {loading ? (
          <p className="p-6 text-sm text-gray-400">加载中…</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
                <th className="px-6 py-3">学校名称</th>
                <th className="px-6 py-3">代码</th>
                <th className="px-6 py-3">状态</th>
                <th className="px-6 py-3">管理员数</th>
                <th className="px-6 py-3">创建时间</th>
                <th className="px-6 py-3 text-right">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {rows.map((school) => (
                <tr key={school.id} className="hover:bg-gray-50">
                  <td className="px-6 py-3 font-medium text-gray-900">{school.name}</td>
                  <td className="px-6 py-3 font-mono text-xs text-gray-500">{school.code}</td>
                  <td className="px-6 py-3">
                    <span
                      className={[
                        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
                        school.status === "ACTIVE"
                          ? "bg-green-100 text-green-700"
                          : "bg-gray-100 text-gray-500",
                      ].join(" ")}
                    >
                      {school.status === "ACTIVE" ? "启用" : "停用"}
                    </span>
                  </td>
                  <td className="px-6 py-3 text-gray-600">{school.adminCount}</td>
                  <td className="px-6 py-3 text-gray-500 text-xs">{school.createdAt.slice(0, 10)}</td>
                  <td className="px-6 py-3 text-right">
                    <div className="inline-flex gap-2">
                      <Link
                        href={`/admin/schools/${school.id}/edit`}
                        className="text-indigo-600 hover:text-indigo-800 text-xs font-medium"
                      >
                        编辑
                      </Link>
                      <Link
                        href={`/admin/schools/${school.id}/accounts`}
                        className="text-indigo-600 hover:text-indigo-800 text-xs font-medium"
                      >
                        账户
                      </Link>
                      <button
                        type="button"
                        onClick={() => handleToggleStatus(school)}
                        className={[
                          "text-xs font-medium",
                          school.status === "ACTIVE"
                            ? "text-red-600 hover:text-red-800"
                            : "text-green-600 hover:text-green-800",
                        ].join(" ")}
                      >
                        {school.status === "ACTIVE" ? "停用" : "启用"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {rows.length === 0 && !loading && (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-sm text-gray-400">
                    暂无学校数据
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
            <span className="px-3 py-1">
              {page + 1} / {totalPages}
            </span>
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
    </div>
  );
}
