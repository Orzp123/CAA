"use client";

import { useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { useAuth } from "@/lib/useAuth";
import SchoolForm from "@/components/school/SchoolForm";

export default function EditSchoolPage() {
  const router = useRouter();
  const params = useParams();
  const { accountType, isAuthenticated } = useAuth();
  const schoolId = typeof params.id === "string" ? params.id : "";

  useEffect(() => {
    if (!isAuthenticated() || accountType !== "SYSTEM_ADMIN") {
      router.replace("/login");
    }
  }, [accountType, isAuthenticated, router]);

  if (accountType !== "SYSTEM_ADMIN") return null;

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">编辑学校</h1>
        <p className="mt-1 text-sm text-gray-500">学校代码创建后不可修改。</p>
      </div>
      <SchoolForm schoolId={schoolId} />
    </div>
  );
}
