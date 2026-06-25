import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import TenantPermissionPage from "../page";
import * as adminApi from "@/lib/adminApi";
import * as authHook from "@/lib/useAuth";

// ── mocks ─────────────────────────────────────────────────────────────────────

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn() }),
}));

vi.mock("@/lib/useAuth", () => ({
  useAuth: vi.fn(),
}));

vi.mock("@/lib/adminApi", () => ({
  tenantAdminApi: {
    list: vi.fn(),
    get:  vi.fn(),
  },
  permissionAdminApi: {
    listAll:                vi.fn(),
    getTenantPermissions:   vi.fn(),
    updateTenantPermissions: vi.fn(),
  },
}));

const mockTenants: adminApi.Tenant[] = [
  { id: "t-1", code: "school-a", name: "学校A", type: "SCHOOL", status: "ACTIVE", defaultLoginType: "PASSWORD" },
  { id: "t-2", code: "school-b", name: "学校B", type: "SCHOOL", status: "ACTIVE", defaultLoginType: "SSO" },
];

const mockPermissions: adminApi.Permission[] = [
  { id: "p-1", code: "AGENT_READ",  name: "Agent 查看", description: null, status: "ACTIVE" },
  { id: "p-2", code: "AGENT_WRITE", name: "Agent 管理", description: null, status: "ACTIVE" },
];

const mockTenantPerms: adminApi.TenantPermission[] = [
  { permissionId: "p-1", permissionCode: "AGENT_READ",  permissionName: "Agent 查看", enabled: true  },
  { permissionId: "p-2", permissionCode: "AGENT_WRITE", permissionName: "Agent 管理", enabled: false },
];

function setupAuth(accountType: string) {
  vi.mocked(authHook.useAuth).mockReturnValue({
    accountType,
    isAuthenticated: () => true,
    token:      "eyJ.test",
    accountId:  "acc-1",
    nickname:   "admin",
    tenantId:   "t-admin",
    tenantName: "CAA",
    expiresAt:  "2099-01-01T00:00:00Z",
    setAuth:    vi.fn(),
    clearAuth:  vi.fn(),
    isExpired:  () => false,
  } as unknown as ReturnType<typeof authHook.useAuth>);
}

beforeEach(() => {
  vi.clearAllMocks();
  setupAuth("SYSTEM_ADMIN");
  vi.mocked(adminApi.tenantAdminApi.list).mockResolvedValue(mockTenants);
  vi.mocked(adminApi.permissionAdminApi.listAll).mockResolvedValue(mockPermissions);
  vi.mocked(adminApi.permissionAdminApi.getTenantPermissions).mockResolvedValue(mockTenantPerms);
});

// ── tests ─────────────────────────────────────────────────────────────────────

describe("TenantPermissionPage", () => {
  it("加载后渲染租户列表", async () => {
    render(<TenantPermissionPage />);
    await waitFor(() => {
      expect(screen.getByText("学校A")).toBeInTheDocument();
      expect(screen.getByText("学校B")).toBeInTheDocument();
    });
  });

  it("渲染权限列表", async () => {
    render(<TenantPermissionPage />);
    await waitFor(() => {
      expect(screen.getByText("Agent 查看")).toBeInTheDocument();
      expect(screen.getByText("Agent 管理")).toBeInTheDocument();
    });
  });

  it("已启用的权限复选框为 checked", async () => {
    render(<TenantPermissionPage />);
    await waitFor(() => {
      const checkboxes = screen.getAllByRole("checkbox");
      expect(checkboxes[0]).toBeChecked();
      expect(checkboxes[1]).not.toBeChecked();
    });
  });

  it("点击行切换权限选中状态", async () => {
    render(<TenantPermissionPage />);
    await waitFor(() => expect(screen.getByText("Agent 管理")).toBeInTheDocument());

    const row = screen.getByText("Agent 管理").closest("tr")!;
    fireEvent.click(row);

    const checkboxes = screen.getAllByRole("checkbox");
    expect(checkboxes[1]).toBeChecked();
  });

  it("点击 Save 调用 updateTenantPermissions", async () => {
    vi.mocked(adminApi.permissionAdminApi.updateTenantPermissions).mockResolvedValue(undefined);
    render(<TenantPermissionPage />);
    // 等待 getTenantPermissions 完成，p-1 的 checkbox 为 checked
    await waitFor(() => {
      const checkboxes = screen.getAllByRole("checkbox");
      expect(checkboxes[0]).toBeChecked();
    });

    fireEvent.click(screen.getByRole("button", { name: /save/i }));

    await waitFor(() => {
      expect(adminApi.permissionAdminApi.updateTenantPermissions).toHaveBeenCalledWith(
        "t-1",
        expect.arrayContaining(["p-1"])
      );
    });
  });

  it("非 SYSTEM_ADMIN 不渲染页面内容", async () => {
    setupAuth("SCHOOL_ADMIN");
    const { container } = render(<TenantPermissionPage />);
    await waitFor(() => {
      expect(container.firstChild).toBeNull();
    });
  });
});
