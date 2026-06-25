import { getTenantConfig, type TenantConfigResponse } from "@/lib/authApi";
import LoginPage from "@/components/auth/LoginPage";

interface SearchParams {
  domain?: string;
  tenantCode?: string;
}

// Fallback config shown when tenant resolution fails (e.g. direct localhost access)
const DEFAULT_TENANT: TenantConfigResponse = {
  tenantId: "default",
  tenantName: "CAA 平台",
  defaultLoginType: "PASSWORD",
  availableLoginTypes: ["PASSWORD"],
  logoUrl: null,
};

export default async function LoginRoute({
  searchParams,
}: {
  searchParams: Promise<SearchParams>;
}) {
  const params = await searchParams;

  let tenantConfig: TenantConfigResponse;

  try {
    tenantConfig = await getTenantConfig({
      domain: params.domain,
      tenantCode: params.tenantCode,
    });
  } catch {
    // Fall back gracefully — e.g. during local dev without a tenant param
    tenantConfig = DEFAULT_TENANT;
  }

  return <LoginPage tenantConfig={tenantConfig} />;
}
