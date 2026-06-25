import axios from "axios";

export const authHttp = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
  timeout: 30000,
});

// ── Types ──────────────────────────────────────────────────────────────────

export interface LoginRequest {
  studentNo: string;
  schoolCode: string;
  password: string;
  captchaUuid: string;
  captchaCode: string;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  accountId: string;
  nickname: string;
  accountType: "SYSTEM_ADMIN" | "SCHOOL_ADMIN" | "TEACHER" | "STUDENT";
  tenantId: string;
  tenantName: string;
}

export interface TenantConfigResponse {
  tenantId: string;
  tenantName: string;
  defaultLoginType: "PASSWORD" | "WECHAT" | "SSO";
  availableLoginTypes: ("PASSWORD" | "WECHAT" | "SSO")[];
  logoUrl: string | null;
}

export interface WechatCallbackResponse {
  requiresProfileCompletion?: boolean;
  tempToken?: string;
  wechatNickname?: string;
  // Present when an existing account is linked — mirrors LoginResponse
  token?: string;
  expiresAt?: string;
  accountId?: string;
  nickname?: string;
  accountType?: "SYSTEM_ADMIN" | "SCHOOL_ADMIN" | "TEACHER" | "STUDENT";
  tenantId?: string;
  tenantName?: string;
}

export interface CompleteProfileRequest {
  tempToken: string;
  studentNo: string;
  name: string;
  nickname: string;
  accountType: "STUDENT" | "TEACHER";
}

// ── API ────────────────────────────────────────────────────────────────────

/**
 * Fetches the CAPTCHA image for the given UUID and returns an object URL.
 * The caller is responsible for calling URL.revokeObjectURL() when done.
 */
export async function getCaptcha(uuid: string): Promise<string> {
  const response = await authHttp.get(`/auth/captcha/${uuid}`, {
    responseType: "blob",
  });
  return URL.createObjectURL(response.data as Blob);
}

export async function login(req: LoginRequest): Promise<LoginResponse> {
  const response = await authHttp.post<LoginResponse>("/auth/login", req);
  return response.data;
}

export async function logout(token: string): Promise<void> {
  await authHttp.post(
    "/auth/logout",
    {},
    { headers: { Authorization: `Bearer ${token}` } }
  );
}

export async function getTenantConfig(params: {
  domain?: string;
  tenantCode?: string;
}): Promise<TenantConfigResponse> {
  const response = await authHttp.get<TenantConfigResponse>(
    "/auth/tenant/config",
    { params }
  );
  return response.data;
}

export async function wechatCallback(
  code: string,
  state: string
): Promise<WechatCallbackResponse> {
  const response = await authHttp.get<WechatCallbackResponse>(
    "/auth/wechat/callback",
    { params: { code, state } }
  );
  return response.data;
}

export async function completeWechatProfile(
  req: CompleteProfileRequest
): Promise<LoginResponse> {
  const response = await authHttp.post<LoginResponse>(
    "/auth/wechat/complete-profile",
    req
  );
  return response.data;
}
