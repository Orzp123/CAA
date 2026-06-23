"use client";

import { useState } from "react";
import type { TenantConfigResponse } from "@/lib/authApi";
import PasswordLoginForm from "./PasswordLoginForm";
import WechatLoginButton from "./WechatLoginButton";
import SsoLoginButton from "./SsoLoginButton";

type LoginType = "PASSWORD" | "WECHAT" | "SSO";

const TAB_LABELS: Record<LoginType, string> = {
  PASSWORD: "账号登录",
  WECHAT: "微信登录",
  SSO: "SSO 认证",
};

interface Props {
  tenantConfig: TenantConfigResponse;
}

export default function LoginPage({ tenantConfig }: Props) {
  const [activeType, setActiveType] = useState<LoginType>(
    tenantConfig.defaultLoginType
  );

  const available = tenantConfig.availableLoginTypes;
  const showTabs = available.length > 1;

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100 px-4 dark:bg-gray-950">
      <div className="w-full max-w-sm">
        {/* Card */}
        <div className="rounded-2xl border border-gray-200 bg-white p-8 shadow-lg dark:border-gray-800 dark:bg-gray-900 dark:shadow-2xl">
          {/* Logo + tenant name */}
          <div className="mb-6 flex flex-col items-center gap-3">
            {tenantConfig.logoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={tenantConfig.logoUrl}
                alt={`${tenantConfig.tenantName} logo`}
                className="h-12 w-auto object-contain"
              />
            ) : (
              <div
                className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-600 text-xl font-bold text-white"
                aria-hidden="true"
              >
                {tenantConfig.tenantName.charAt(0)}
              </div>
            )}
            <h1 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
              {tenantConfig.tenantName}
            </h1>
            <p className="text-sm text-gray-500 dark:text-gray-400">欢迎回来，请登录您的账号</p>
          </div>

          {/* Login type tabs */}
          {showTabs && (
            <div
              role="tablist"
              aria-label="登录方式"
              className="mb-6 flex rounded-lg bg-gray-100 p-1 dark:bg-gray-800"
            >
              {available.map((type) => (
                <button
                  key={type}
                  role="tab"
                  aria-selected={activeType === type}
                  aria-controls={`panel-${type}`}
                  onClick={() => setActiveType(type)}
                  className={`flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition ${
                    activeType === type
                      ? "bg-white text-gray-900 shadow-sm dark:bg-gray-700 dark:text-gray-100"
                      : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
                  }`}
                >
                  {TAB_LABELS[type]}
                </button>
              ))}
            </div>
          )}

          {/* Active panel */}
          <div
            id={`panel-${activeType}`}
            role="tabpanel"
            aria-label={TAB_LABELS[activeType]}
          >
            {activeType === "PASSWORD" && (
              <PasswordLoginForm tenantConfig={tenantConfig} />
            )}
            {activeType === "WECHAT" && (
              <div className="flex flex-col gap-4">
                <p className="text-center text-sm text-gray-500 dark:text-gray-400">
                  使用微信扫码快速登录
                </p>
                <WechatLoginButton tenantCode={tenantConfig.tenantId} />
              </div>
            )}
            {activeType === "SSO" && (
              <div className="flex flex-col gap-4">
                <p className="text-center text-sm text-gray-500 dark:text-gray-400">
                  通过机构统一身份认证登录
                </p>
                <SsoLoginButton tenantCode={tenantConfig.tenantId} />
              </div>
            )}
          </div>
        </div>

        <p className="mt-6 text-center text-xs text-gray-400 dark:text-gray-600">
          © {new Date().getFullYear()} CAA · Claude Agent Assembly
        </p>
      </div>
    </div>
  );
}
