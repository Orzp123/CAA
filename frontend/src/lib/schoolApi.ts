import { createAuthenticatedClient } from "./httpClient";

// ── Authenticated HTTP client ─────────────────────────────────────────────────

const schoolHttp = createAuthenticatedClient();

// ── Types ─────────────────────────────────────────────────────────────────────

export type SchoolStatus = "ACTIVE" | "INACTIVE";
export type AccountStatus = "ACTIVE" | "DISABLED";
export type AccountType = "SCHOOL_ADMIN" | "TEACHER" | "STUDENT";
export type SecondaryRole = "TEACHER" | "ASSISTANT";
export type SlotPosition = "HOME_TOP_BANNER" | "HOME_SIDEBAR" | "LOGIN_BANNER";

export interface SchoolBrand {
  logoUrl?: string | null;
  systemNameZh: string;
  systemNameEn: string;
  description: string | null;
}

export interface PromotionalSlot {
  title: string;
  imageUrl: string;
  linkUrl: string;
  position: SlotPosition;
  sortOrder: number;
}

/** 列表行（轻量） */
export interface SchoolRow {
  id: string;
  code: string;
  name: string;
  domain?: string;
  status: SchoolStatus;
  adminCount: number;
  createdAt: string;
}

/** 详情（含品牌、套餐、权限、运营位） */
export interface SchoolDetail extends SchoolRow {
  brand: SchoolBrand;
  packageId: string | null;
  permissionCodes: string[];
  slots: PromotionalSlot[];
}

export interface SchoolCreateRequest {
  name: string;
  code: string;
  domain: string;
  packageId: string;
  permissionCodes: string[];
  brand: SchoolBrand;
  slots: PromotionalSlot[];
}

export interface SchoolCreateResponse {
  schoolId: string;
  code: string;
  name: string;
  defaultAdminLoginName: string;
  status: SchoolStatus;
  createdAt: string;
}

export interface SchoolUpdateRequest {
  name: string;
  domain: string;
  packageId: string;
  permissionCodes: string[];
  brand: SchoolBrand;
  slots: PromotionalSlot[];
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface SchoolListParams {
  page: number;
  size: number;
  name?: string;
  code?: string;
}

export interface LogoUploadUrlResponse {
  uploadUrl: string;
  logoPath: string;
  expiresIn: number;
}

export interface AccountRow {
  id: string;
  loginName: string;
  name: string;
  nickname: string | null;
  email: string | null;
  phone: string | null;
  accountType: AccountType;
  secondaryRole: SecondaryRole | null;
  status: AccountStatus;
  createdAt: string;
}

export interface AccountCreateRequest {
  loginName: string;
  name: string;
  accountType: AccountType;
  secondaryRole: SecondaryRole | null;
  nickname: string | null;
  email: string | null;
  phone: string | null;
  password: string | null;
}

export interface AccountUpdateRequest {
  name: string;
  nickname: string | null;
  email: string | null;
  phone: string | null;
}

export interface AccountListParams {
  page: number;
  size: number;
  loginName?: string;
  accountType?: AccountType;
  name?: string;
}

export interface BatchImportResult {
  total: number;
  successCount: number;
  failureCount: number;
  reportDownloadUrl: string;
}

export interface BatchRemoveResult {
  deletedCount: number;
  excludedSelfCount: number;
}

export interface BenefitPackage {
  id: string;
  name: string;
  defaultPermissionCodes: string[];
}

// ── schoolApi ─────────────────────────────────────────────────────────────────

export const schoolApi = {
  /** GET /v1/schools — 学校列表（分页+搜索） */
  list: (params: SchoolListParams): Promise<PageResult<SchoolRow>> => {
    const query: Record<string, unknown> = { page: params.page, size: params.size };
    if (params.name) query.name = params.name;
    if (params.code) query.code = params.code;
    return schoolHttp
      .get<{ data: PageResult<SchoolRow> }>("/v1/schools", { params: query })
      .then((r) => r.data.data);
  },

  /** GET /v1/schools/:id — 学校详情 */
  get: (id: string): Promise<SchoolDetail> =>
    schoolHttp
      .get<{ data: SchoolDetail }>(`/v1/schools/${id}`)
      .then((r) => r.data.data),

  /** POST /v1/schools — 新建学校 */
  create: (req: SchoolCreateRequest): Promise<SchoolCreateResponse> =>
    schoolHttp
      .post<{ data: SchoolCreateResponse }>("/v1/schools", req)
      .then((r) => r.data.data),

  /** PUT /v1/schools/:id — 更新学校 */
  update: (id: string, req: SchoolUpdateRequest): Promise<SchoolDetail> =>
    schoolHttp
      .put<{ data: SchoolDetail }>(`/v1/schools/${id}`, req)
      .then((r) => r.data.data),

  /** PATCH /v1/schools/:id/status — 停用/启用学校 */
  patchStatus: (id: string, status: SchoolStatus): Promise<{ id: string; status: SchoolStatus }> =>
    schoolHttp
      .patch<{ data: { id: string; status: SchoolStatus } }>(`/v1/schools/${id}/status`, { status })
      .then((r) => r.data.data),

  /** GET /v1/schools/:id/logo-upload-url — 获取 Logo 预签名上传 URL */
  getLogoUploadUrl: (id: string): Promise<LogoUploadUrlResponse> =>
    schoolHttp
      .get<{ data: LogoUploadUrlResponse }>(`/v1/schools/${id}/logo-upload-url`)
      .then((r) => r.data.data),
};

// ── accountApi ────────────────────────────────────────────────────────────────

export const accountApi = {
  /** GET /v1/schools/:schoolId/accounts — 账户列表 */
  list: (schoolId: string, params: AccountListParams): Promise<PageResult<AccountRow>> => {
    const query: Record<string, unknown> = { page: params.page, size: params.size };
    if (params.loginName) query.loginName = params.loginName;
    if (params.accountType) query.accountType = params.accountType;
    if (params.name) query.name = params.name;
    return schoolHttp
      .get<{ data: PageResult<AccountRow> }>(`/v1/schools/${schoolId}/accounts`, { params: query })
      .then((r) => r.data.data);
  },

  /** POST /v1/schools/:schoolId/accounts — 创建账户 */
  create: (schoolId: string, req: AccountCreateRequest): Promise<AccountRow> =>
    schoolHttp
      .post<{ data: AccountRow }>(`/v1/schools/${schoolId}/accounts`, req)
      .then((r) => r.data.data),

  /** PUT /v1/schools/:schoolId/accounts/:accountId — 编辑账户 */
  update: (schoolId: string, accountId: string, req: AccountUpdateRequest): Promise<AccountRow> =>
    schoolHttp
      .put<{ data: AccountRow }>(`/v1/schools/${schoolId}/accounts/${accountId}`, req)
      .then((r) => r.data.data),

  /** PATCH /v1/schools/:schoolId/accounts/:accountId/password — 重置密码 */
  resetPassword: (schoolId: string, accountId: string, password: string | null): Promise<void> =>
    schoolHttp
      .patch(`/v1/schools/${schoolId}/accounts/${accountId}/password`, { password })
      .then(() => undefined),

  /** PATCH /v1/schools/:schoolId/accounts/:accountId/status — 启用/停用账户 */
  patchStatus: (
    schoolId: string,
    accountId: string,
    status: AccountStatus
  ): Promise<{ id: string; status: AccountStatus }> =>
    schoolHttp
      .patch<{ data: { id: string; status: AccountStatus } }>(
        `/v1/schools/${schoolId}/accounts/${accountId}/status`,
        { status }
      )
      .then((r) => r.data.data),

  /** DELETE /v1/schools/:schoolId/accounts/:accountId — 删除单个账户 */
  remove: (schoolId: string, accountId: string): Promise<void> =>
    schoolHttp
      .delete(`/v1/schools/${schoolId}/accounts/${accountId}`)
      .then(() => undefined),

  /** POST /v1/schools/:schoolId/accounts/batch-import — 批量导入 */
  batchImport: (schoolId: string, file: File): Promise<BatchImportResult> => {
    const form = new FormData();
    form.append("file", file);
    return schoolHttp
      .post<{ data: BatchImportResult }>(
        `/v1/schools/${schoolId}/accounts/batch-import`,
        form,
        { headers: { "Content-Type": "multipart/form-data" } }
      )
      .then((r) => r.data.data);
  },

  /** PATCH /v1/schools/:schoolId/accounts/batch-status — 批量修改状态 */
  batchPatchStatus: (
    schoolId: string,
    accountIds: string[],
    status: AccountStatus
  ): Promise<void> =>
    schoolHttp
      .patch(`/v1/schools/${schoolId}/accounts/batch-status`, { accountIds, status })
      .then(() => undefined),

  /** DELETE /v1/schools/:schoolId/accounts/batch — 批量删除 */
  batchRemove: (schoolId: string, accountIds: string[]): Promise<BatchRemoveResult> =>
    schoolHttp
      .delete<{ data: BatchRemoveResult }>(`/v1/schools/${schoolId}/accounts/batch`, {
        data: { accountIds },
      })
      .then((r) => r.data.data),
};

// ── benefitPackageApi ─────────────────────────────────────────────────────────

export const benefitPackageApi = {
  /** GET /v1/benefit-packages — 获取可用套餐列表 */
  list: (): Promise<BenefitPackage[]> =>
    schoolHttp
      .get<{ data: BenefitPackage[] }>("/v1/benefit-packages")
      .then((r) => r.data.data),
};
