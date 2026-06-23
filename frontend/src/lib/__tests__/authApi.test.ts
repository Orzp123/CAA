import { describe, it, expect, beforeEach, afterEach } from "vitest";
import MockAdapter from "axios-mock-adapter";
import axios from "axios";
import {
  login,
  logout,
  getTenantConfig,
  type LoginRequest,
  type LoginResponse,
  type TenantConfigResponse,
} from "../authApi";

const mock = new MockAdapter(axios);

beforeEach(() => mock.reset());
afterEach(() => mock.reset());

// ── login ────────────────────────────────────────────────────────────────────

describe("login", () => {
  const req: LoginRequest = {
    studentNo:   "20240001",
    schoolCode:  "school-test",
    password:    "Test@123456",
    captchaUuid: "uuid-abc",
    captchaCode: "1234",
  };

  const resp: LoginResponse = {
    token:       "eyJ.test.token",
    expiresAt:   "2099-01-01T00:00:00Z",
    accountId:   "acc-1",
    nickname:    "张三",
    accountType: "STUDENT",
    tenantId:    "tenant-1",
    tenantName:  "测试学校",
  };

  it("成功登录返回 LoginResponse", async () => {
    mock.onPost("/api/auth/login").reply(200, resp);
    const result = await login(req);
    expect(result.token).toBe(resp.token);
    expect(result.accountId).toBe(resp.accountId);
    expect(result.accountType).toBe("STUDENT");
  });

  it("400 错误时抛出异常", async () => {
    mock.onPost("/api/auth/login").reply(400, { message: "验证码错误" });
    await expect(login(req)).rejects.toThrow();
  });

  it("401 错误时抛出异常", async () => {
    mock.onPost("/api/auth/login").reply(401, { message: "密码错误" });
    await expect(login(req)).rejects.toThrow();
  });
});

// ── logout ───────────────────────────────────────────────────────────────────

describe("logout", () => {
  it("成功登出不抛出异常", async () => {
    mock.onPost("/api/auth/logout").reply(200);
    await expect(logout("eyJ.test.token")).resolves.toBeUndefined();
  });

  it("服务端错误时抛出异常", async () => {
    mock.onPost("/api/auth/logout").reply(500);
    await expect(logout("eyJ.test.token")).rejects.toThrow();
  });
});

// ── getTenantConfig ───────────────────────────────────────────────────────────

describe("getTenantConfig", () => {
  const resp: TenantConfigResponse = {
    tenantId:            "tenant-1",
    tenantName:          "测试学校",
    defaultLoginType:    "PASSWORD",
    availableLoginTypes: ["PASSWORD", "SSO"],
    logoUrl:             null,
  };

  it("通过 tenantCode 获取租户配置", async () => {
    mock.onGet("/api/auth/tenant/config").reply(200, resp);
    const result = await getTenantConfig({ tenantCode: "school-test" });
    expect(result.tenantId).toBe("tenant-1");
    expect(result.availableLoginTypes).toContain("PASSWORD");
  });

  it("通过 domain 获取租户配置", async () => {
    mock.onGet("/api/auth/tenant/config").reply(200, resp);
    const result = await getTenantConfig({ domain: "school-test.example.com" });
    expect(result.defaultLoginType).toBe("PASSWORD");
    expect(result.logoUrl).toBeNull();
  });

  it("404 时抛出异常", async () => {
    mock.onGet("/api/auth/tenant/config").reply(404);
    await expect(getTenantConfig({ tenantCode: "unknown" })).rejects.toThrow();
  });
});
