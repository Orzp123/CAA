"use client";

import { useState, useRef } from "react";
import { accountApi, type BatchImportResult } from "@/lib/schoolApi";

interface Props {
  schoolId: string;
  onClose: () => void;
}

type UploadState = "idle" | "uploading" | "done" | "error";

export default function BatchImportDialog({ schoolId, onClose }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [uploadState, setUploadState] = useState<UploadState>("idle");
  const [result, setResult] = useState<BatchImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0] ?? null;
    setFile(f);
    setResult(null);
    setError(null);
    setUploadState("idle");
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    const f = e.dataTransfer.files[0];
    if (!f) return;
    const ext = f.name.split(".").pop()?.toLowerCase();
    if (ext !== "xlsx" && ext !== "csv") {
      setError("仅支持 .xlsx / .csv 格式");
      return;
    }
    setFile(f);
    setResult(null);
    setError(null);
    setUploadState("idle");
  }

  async function handleUpload() {
    if (!file) return;
    const ext = file.name.split(".").pop()?.toLowerCase();
    if (ext !== "xlsx" && ext !== "csv") {
      setError("仅支持 .xlsx / .csv 格式");
      return;
    }

    setUploadState("uploading");
    setError(null);
    try {
      const res = await accountApi.batchImport(schoolId, file);
      setResult(res);
      setUploadState("done");
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      const msg = e?.response?.data?.message;
      setError(msg ?? "导入失败，请检查文件格式或稍后重试。");
      setUploadState("error");
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="批量导入账户"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg mx-4 overflow-hidden">
        {/* 标题栏 */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h2 className="text-base font-semibold text-gray-900">批量导入账户</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="关闭"
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            ✕
          </button>
        </div>

        <div className="px-6 py-5 space-y-4">
          {/* 说明 */}
          <p className="text-sm text-gray-600">
            支持 <span className="font-mono">.xlsx</span> / <span className="font-mono">.csv</span> 格式，有效数据行数上限 1000 条（不含表头）。
            登录名已存在的行将记入错误报告。
          </p>

          {/* 拖拽区域 */}
          <div
            role="button"
            tabIndex={0}
            onDragOver={(e) => e.preventDefault()}
            onDrop={handleDrop}
            onClick={() => inputRef.current?.click()}
            onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") inputRef.current?.click(); }}
            className="cursor-pointer rounded-lg border-2 border-dashed border-gray-300 px-6 py-8 text-center hover:border-indigo-400 transition-colors"
          >
            <input
              ref={inputRef}
              type="file"
              accept=".xlsx,.csv"
              onChange={handleFileChange}
              className="sr-only"
              aria-label="选择导入文件"
            />
            {file ? (
              <div>
                <p className="text-sm font-medium text-gray-900">{file.name}</p>
                <p className="text-xs text-gray-400 mt-1">
                  {(file.size / 1024).toFixed(1)} KB · 点击更换文件
                </p>
              </div>
            ) : (
              <div>
                <p className="text-sm text-gray-500">拖拽文件到此处，或点击选择文件</p>
                <p className="text-xs text-gray-400 mt-1">.xlsx / .csv</p>
              </div>
            )}
          </div>

          {/* 错误 */}
          {error && (
            <div role="alert" className="rounded-md bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          )}

          {/* 导入结果 */}
          {result && uploadState === "done" && (
            <div className="rounded-md bg-gray-50 border border-gray-200 px-4 py-3 space-y-2">
              <p className="text-sm font-semibold text-gray-700">导入完成</p>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>总行数：<span className="font-medium">{result.total}</span></li>
                <li className="text-green-700">
                  成功：<span className="font-medium">{result.successCount}</span>
                </li>
                {result.failureCount > 0 && (
                  <li className="text-red-700">
                    失败：<span className="font-medium">{result.failureCount}</span>
                  </li>
                )}
              </ul>
              {result.failureCount > 0 && (
                <a
                  href={result.reportDownloadUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-block text-sm text-indigo-600 hover:text-indigo-800 underline"
                >
                  下载错误报告（Excel）
                </a>
              )}
            </div>
          )}

          {/* 操作按钮 */}
          <div className="flex gap-3 pt-1">
            <button
              type="button"
              onClick={handleUpload}
              disabled={!file || uploadState === "uploading" || uploadState === "done"}
              className="flex-1 py-2 rounded-md bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {uploadState === "uploading" ? "导入中…" : "开始导入"}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2 rounded-md border border-gray-300 text-gray-600 text-sm hover:bg-gray-100 transition-colors"
            >
              {uploadState === "done" ? "关闭" : "取消"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
