"use client";

import { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/lib/useAuth";
import { schoolApi } from "@/lib/schoolApi";
import BatchImportDialog from "@/components/school/BatchImportDialog";

export default function BatchImportPage() {
  const router = useRouter();
  const params = useParams();
  const { isAuthenticated } = useAuth();
  const schoolId = typeof params.id === "string" ? params.id : "";

  const [schoolName, setSchoolName] = useState("");

  useEffect(() => {
    if (!isAuthenticated()) {
      router.replace("/login");
    }
  }, [isAuthenticated, router]);

  useEffect(() => {
    if (!schoolId) return;
    let cancelled = false;
    schoolApi.get(schoolId).then((d) => { if (!cancelled) setSchoolName(d.name); }).catch(() => {});
    return () => { cancelled = true; };
  }, [schoolId]);

  function handleClose() {
    router.push(`/admin/schools/${schoolId}/accounts`);
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="flex items-center gap-3 mb-4">
        <Link
          href={`/admin/schools/${schoolId}/accounts`}
          className="text-sm text-indigo-600 hover:text-indigo-800"
        >
          ← 账户管理
        </Link>
        {schoolName && (
          <span className="text-sm text-gray-400">/ {schoolName}</span>
        )}
      </div>
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">批量导入账户</h1>
      {/* 内联展示 Dialog 内容（非 overlay 模式） */}
      <div className="max-w-lg">
        <BatchImportDialog schoolId={schoolId} onClose={handleClose} />
      </div>
    </div>
  );
}
