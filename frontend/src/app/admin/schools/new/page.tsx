"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/useAuth";
import SchoolForm from "@/components/school/SchoolForm";

export default function NewSchoolPage() {
  const router = useRouter();
  const { accountType, isAuthenticated } = useAuth();

  useEffect(() => {
    if (!isAuthenticated() || accountType !== "SYSTEM_ADMIN") {
      router.replace("/login");
    }
  }, [accountType, isAuthenticated, router]);

  if (accountType !== "SYSTEM_ADMIN") return null;

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">新建学校</h1>
        <p className="mt-1 text-sm text-gray-500">
          创建后将自动生成默认管理员账号（admin_&lt;CODE&gt;）。
        </p>
      </div>
      <SchoolForm />
    </div>
  );
}
