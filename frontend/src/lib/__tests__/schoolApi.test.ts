import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";

// ── mock axios ────────────────────────────────────────────────────────────────
vi.mock("axios", async () => {
  const actual = await vi.importActual<typeof import("axios")>("axios");
  const instance = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request:  { use: vi.fn() },
      response: { use: vi.fn() },
    },
  };
  return {
    ...actual,
    default: { ...actual.default, create: vi.fn(() => instance) },
    __instance: instance,
  };
});

// 动态取 mock instance，必须在 vi.mock 后导入模块
const getAxiosInstance = async () => {
  const mod = await import("axios");
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (mod as any).__instance as Record<string, ReturnType<typeof vi.fn>>;
};

// mock localStorage
beforeEach(() => {
  Object.defineProperty(globalThis, "localStorage", {
    value: {
      getItem: vi.fn(() => JSON.stringify({ token: "test-token" })),
      setItem: vi.fn(),
      removeItem: vi.fn(),
    },
    writable: true,
  });
});

afterEach(() => {
  vi.clearAllMocks();
});

// ── helpers ───────────────────────────────────────────────────────────────────

function makePageResponse<T>(content: T[]) {
  return {
    data: {
      success: true,
      data: {
        content,
        totalElements: content.length,
        totalPages: 1,
        page: 0,
        size: 20,
      },
    },
  };
}

function makeResponse<T>(data: T) {
  return { data: { success: true, data } };
}

// ── schoolApi tests ───────────────────────────────────────────────────────────

describe("schoolApi", () => {
  describe("list", () => {
    it("GET /schools 并返回分页数据", async () => {
      const inst = await getAxiosInstance();
      const school = {
        id: "s-1",
        code: "TEST",
        name: "测试大学",
        status: "ACTIVE" as const,
        adminCount: 2,
        createdAt: "2026-06-25T10:00:00",
      };
      inst.get.mockResolvedValue(makePageResponse([school]));

      const { schoolApi } = await import("../schoolApi");
      const result = await schoolApi.list({ page: 0, size: 20 });

      expect(inst.get).toHaveBeenCalledWith("/v1/schools", {
        params: { page: 0, size: 20 },
      });
      expect(result.content).toHaveLength(1);
      expect(result.content[0].code).toBe("TEST");
    });

    it("传递 name/code 搜索参数", async () => {
      const inst = await getAxiosInstance();
      inst.get.mockResolvedValue(makePageResponse([]));

      const { schoolApi } = await import("../schoolApi");
      await schoolApi.list({ page: 0, size: 20, name: "北大", code: "PKU" });

      expect(inst.get).toHaveBeenCalledWith("/v1/schools", {
        params: { page: 0, size: 20, name: "北大", code: "PKU" },
      });
    });
  });

  describe("get", () => {
    it("GET /schools/:id 并返回详情", async () => {
      const inst = await getAxiosInstance();
      const detail = {
        id: "s-1",
        code: "TEST",
        name: "测试大学",
        status: "ACTIVE" as const,
        adminCount: 1,
        createdAt: "2026-06-25T10:00:00",
        brand: { systemNameZh: "平台", systemNameEn: "Platform", description: null },
        permissionCodes: ["AGENT_READ"],
        slots: [],
      };
      inst.get.mockResolvedValue(makeResponse(detail));

      const { schoolApi } = await import("../schoolApi");
      const result = await schoolApi.get("s-1");

      expect(inst.get).toHaveBeenCalledWith("/v1/schools/s-1");
      expect(result.name).toBe("测试大学");
    });
  });

  describe("create", () => {
    it("POST /schools 并返回创建结果", async () => {
      const inst = await getAxiosInstance();
      const created = {
        schoolId: "s-new",
        code: "NEW",
        name: "新大学",
        defaultAdminLoginName: "admin_NEW",
        status: "ACTIVE" as const,
        createdAt: "2026-06-25T10:00:00",
      };
      inst.post.mockResolvedValue({ data: { success: true, data: created }, status: 201 });

      const { schoolApi } = await import("../schoolApi");
      const result = await schoolApi.create({
        name: "新大学",
        code: "NEW",
        domain: "new.edu.cn",
        packageId: "pkg-1",
        permissionCodes: [],
        brand: { systemNameZh: "", systemNameEn: "", description: null },
        slots: [],
      });

      expect(inst.post).toHaveBeenCalledWith("/v1/schools", expect.objectContaining({ code: "NEW" }));
      expect(result.schoolId).toBe("s-new");
    });
  });

  describe("update", () => {
    it("PUT /schools/:id 并返回更新结果", async () => {
      const inst = await getAxiosInstance();
      inst.put.mockResolvedValue(makeResponse({ id: "s-1", name: "已更新大学" }));

      const { schoolApi } = await import("../schoolApi");
      await schoolApi.update("s-1", {
        name: "已更新大学",
        domain: "updated.edu.cn",
        packageId: "pkg-1",
        permissionCodes: [],
        brand: { systemNameZh: "", systemNameEn: "", description: null },
        slots: [],
      });

      expect(inst.put).toHaveBeenCalledWith("/v1/schools/s-1", expect.objectContaining({ name: "已更新大学" }));
    });
  });

  describe("patchStatus", () => {
    it("PATCH /schools/:id/status", async () => {
      const inst = await getAxiosInstance();
      inst.patch.mockResolvedValue(makeResponse({ id: "s-1", status: "INACTIVE" }));

      const { schoolApi } = await import("../schoolApi");
      const result = await schoolApi.patchStatus("s-1", "INACTIVE");

      expect(inst.patch).toHaveBeenCalledWith("/v1/schools/s-1/status", { status: "INACTIVE" });
      expect(result.status).toBe("INACTIVE");
    });
  });

  describe("getLogoUploadUrl", () => {
    it("GET /schools/:id/logo-upload-url", async () => {
      const inst = await getAxiosInstance();
      inst.get.mockResolvedValue(
        makeResponse({ uploadUrl: "https://minio/presigned", logoPath: "schools/s-1/logo.png", expiresIn: 300 })
      );

      const { schoolApi } = await import("../schoolApi");
      const result = await schoolApi.getLogoUploadUrl("s-1");

      expect(inst.get).toHaveBeenCalledWith("/v1/schools/s-1/logo-upload-url");
      expect(result.logoPath).toBe("schools/s-1/logo.png");
    });
  });
});

// ── accountApi tests ──────────────────────────────────────────────────────────

describe("accountApi", () => {
  describe("list", () => {
    it("GET /schools/:schoolId/accounts 并返回分页数据", async () => {
      const inst = await getAxiosInstance();
      const account = {
        id: "a-1",
        loginName: "s2026001",
        name: "张三",
        accountType: "STUDENT" as const,
        secondaryRole: null,
        status: "ACTIVE" as const,
        createdAt: "2026-06-25T10:00:00",
      };
      inst.get.mockResolvedValue(makePageResponse([account]));

      const { accountApi } = await import("../schoolApi");
      const result = await accountApi.list("s-1", { page: 0, size: 20 });

      expect(inst.get).toHaveBeenCalledWith("/v1/schools/s-1/accounts", {
        params: { page: 0, size: 20 },
      });
      expect(result.content[0].loginName).toBe("s2026001");
    });
  });

  describe("create", () => {
    it("POST /schools/:schoolId/accounts", async () => {
      const inst = await getAxiosInstance();
      const created = {
        id: "a-new",
        loginName: "s2026002",
        name: "李四",
        accountType: "STUDENT" as const,
        secondaryRole: null,
        status: "ACTIVE" as const,
        createdAt: "2026-06-25T10:00:00",
      };
      inst.post.mockResolvedValue({ data: { success: true, data: created }, status: 201 });

      const { accountApi } = await import("../schoolApi");
      const result = await accountApi.create("s-1", {
        loginName: "s2026002",
        name: "李四",
        accountType: "STUDENT",
        secondaryRole: null,
        nickname: null,
        email: null,
        phone: null,
        password: null,
      });

      expect(inst.post).toHaveBeenCalledWith("/v1/schools/s-1/accounts", expect.objectContaining({ loginName: "s2026002" }));
      expect(result.id).toBe("a-new");
    });
  });

  describe("update", () => {
    it("PUT /schools/:schoolId/accounts/:accountId", async () => {
      const inst = await getAxiosInstance();
      inst.put.mockResolvedValue(makeResponse({ id: "a-1" }));

      const { accountApi } = await import("../schoolApi");
      await accountApi.update("s-1", "a-1", { name: "张三改", nickname: null, email: null, phone: null });

      expect(inst.put).toHaveBeenCalledWith(
        "/v1/schools/s-1/accounts/a-1",
        expect.objectContaining({ name: "张三改" })
      );
    });
  });

  describe("resetPassword", () => {
    it("PATCH /schools/:schoolId/accounts/:accountId/password", async () => {
      const inst = await getAxiosInstance();
      inst.patch.mockResolvedValue(makeResponse(null));

      const { accountApi } = await import("../schoolApi");
      await accountApi.resetPassword("s-1", "a-1", "NewPass123");

      expect(inst.patch).toHaveBeenCalledWith(
        "/v1/schools/s-1/accounts/a-1/password",
        { password: "NewPass123" }
      );
    });
  });

  describe("patchStatus", () => {
    it("PATCH /schools/:schoolId/accounts/:accountId/status", async () => {
      const inst = await getAxiosInstance();
      inst.patch.mockResolvedValue(makeResponse({ id: "a-1", status: "DISABLED" }));

      const { accountApi } = await import("../schoolApi");
      const result = await accountApi.patchStatus("s-1", "a-1", "DISABLED");

      expect(inst.patch).toHaveBeenCalledWith(
        "/v1/schools/s-1/accounts/a-1/status",
        { status: "DISABLED" }
      );
      expect(result.status).toBe("DISABLED");
    });
  });

  describe("remove", () => {
    it("DELETE /schools/:schoolId/accounts/:accountId", async () => {
      const inst = await getAxiosInstance();
      inst.delete.mockResolvedValue(makeResponse(null));

      const { accountApi } = await import("../schoolApi");
      await accountApi.remove("s-1", "a-1");

      expect(inst.delete).toHaveBeenCalledWith("/v1/schools/s-1/accounts/a-1");
    });
  });

  describe("batchImport", () => {
    it("POST /schools/:schoolId/accounts/batch-import multipart", async () => {
      const inst = await getAxiosInstance();
      inst.post.mockResolvedValue(
        makeResponse({ total: 10, successCount: 9, failureCount: 1, reportDownloadUrl: "https://minio/report" })
      );

      const file = new File(["col1,col2"], "import.csv", { type: "text/csv" });
      const { accountApi } = await import("../schoolApi");
      const result = await accountApi.batchImport("s-1", file);

      expect(inst.post).toHaveBeenCalledWith(
        "/v1/schools/s-1/accounts/batch-import",
        expect.any(FormData),
        expect.objectContaining({ headers: { "Content-Type": "multipart/form-data" } })
      );
      expect(result.successCount).toBe(9);
    });
  });

  describe("batchPatchStatus", () => {
    it("PATCH /schools/:schoolId/accounts/batch-status", async () => {
      const inst = await getAxiosInstance();
      inst.patch.mockResolvedValue(makeResponse(null));

      const { accountApi } = await import("../schoolApi");
      await accountApi.batchPatchStatus("s-1", ["a-1", "a-2"], "DISABLED");

      expect(inst.patch).toHaveBeenCalledWith(
        "/v1/schools/s-1/accounts/batch-status",
        { accountIds: ["a-1", "a-2"], status: "DISABLED" }
      );
    });
  });

  describe("batchRemove", () => {
    it("DELETE /schools/:schoolId/accounts/batch", async () => {
      const inst = await getAxiosInstance();
      inst.delete.mockResolvedValue(makeResponse({ deletedCount: 2, excludedSelfCount: 0 }));

      const { accountApi } = await import("../schoolApi");
      const result = await accountApi.batchRemove("s-1", ["a-1", "a-2"]);

      expect(inst.delete).toHaveBeenCalledWith(
        "/v1/schools/s-1/accounts/batch",
        { data: { accountIds: ["a-1", "a-2"] } }
      );
      expect(result.deletedCount).toBe(2);
    });
  });
});

// ── benefitPackageApi tests ───────────────────────────────────────────────────

describe("benefitPackageApi", () => {
  it("GET /benefit-packages 并返回套餐列表", async () => {
    const inst = await getAxiosInstance();
    const pkg = { id: "pkg-1", name: "标准版", defaultPermissionCodes: ["AGENT_READ"] };
    inst.get.mockResolvedValue(makeResponse([pkg]));

    const { benefitPackageApi } = await import("../schoolApi");
    const result = await benefitPackageApi.list();

    expect(inst.get).toHaveBeenCalledWith("/v1/benefit-packages");
    expect(result[0].name).toBe("标准版");
  });
});
