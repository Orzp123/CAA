"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useRouter } from "next/navigation";
import axios from "axios";
import { login, getCaptcha } from "@/lib/authApi";
import { useAuth } from "@/lib/useAuth";
import type { TenantConfigResponse } from "@/lib/authApi";

interface Props {
  tenantConfig: TenantConfigResponse;
}

function generateUuid(): string {
  return crypto.randomUUID();
}

export default function PasswordLoginForm({ tenantConfig }: Props) {
  const router = useRouter();
  const setAuth = useAuth((s) => s.setAuth);

  const [studentNo, setStudentNo] = useState("");
  const [password, setPassword] = useState("");
  const [captchaCode, setCaptchaCode] = useState("");
  const [captchaUuid, setCaptchaUuid] = useState(() => generateUuid());
  const [captchaUrl, setCaptchaUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Track the current blob URL in a ref so cleanup always has the latest value
  const captchaUrlRef = useRef<string | null>(null);

  function setAndTrackCaptchaUrl(url: string | null) {
    captchaUrlRef.current = url;
    setCaptchaUrl(url);
  }

  // Load captcha on mount; revoke blob URL on unmount
  useEffect(() => {
    let cancelled = false;
    getCaptcha(captchaUuid)
      .then((url) => {
        if (!cancelled) setAndTrackCaptchaUrl(url);
        else URL.revokeObjectURL(url);
      })
      .catch(() => {
        if (!cancelled) setAndTrackCaptchaUrl(null);
      });

    return () => {
      cancelled = true;
      if (captchaUrlRef.current) {
        URL.revokeObjectURL(captchaUrlRef.current);
        captchaUrlRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // intentionally runs once on mount

  const refreshCaptcha = useCallback(async () => {
    const uuid = generateUuid();
    setCaptchaUuid(uuid);
    setCaptchaCode("");

    // Revoke the current blob URL before fetching a new one
    if (captchaUrlRef.current) {
      URL.revokeObjectURL(captchaUrlRef.current);
      captchaUrlRef.current = null;
    }
    setCaptchaUrl(null);

    try {
      const url = await getCaptcha(uuid);
      setAndTrackCaptchaUrl(url);
    } catch {
      setAndTrackCaptchaUrl(null);
    }
  }, []);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const response = await login({
        studentNo,
        schoolCode: tenantConfig.tenantId,
        password,
        captchaUuid,
        captchaCode,
      });
      setAuth(response);
      router.push("/");
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const status = err.response?.status;
        const data = err.response?.data as
          | { message?: string; lockedUntil?: string }
          | undefined;

        if (status === 423) {
          const until = data?.lockedUntil
            ? new Date(data.lockedUntil).toLocaleTimeString()
            : "";
          setError(`账号已被锁定，请于 ${until} 后重试`);
        } else if (status === 403) {
          setError("账号已被禁用，请联系管理员");
        } else if (status === 401) {
          setError("账号或密码错误");
        } else {
          setError(data?.message ?? "登录失败，请稍后重试");
        }
      } else {
        setError("网络异常，请稍后重试");
      }
      await refreshCaptcha();
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
      {error && (
        <div
          role="alert"
          className="rounded-lg bg-red-950 border border-red-800 px-4 py-3 text-sm text-red-300"
        >
          {error}
        </div>
      )}

      <div className="flex flex-col gap-1.5">
        <label htmlFor="studentNo" className="text-sm font-medium text-gray-300">
          学号 / 工号
        </label>
        <input
          id="studentNo"
          type="text"
          autoComplete="username"
          required
          value={studentNo}
          onChange={(e) => setStudentNo(e.target.value)}
          placeholder="请输入学号或工号"
          className="rounded-lg border border-gray-700 bg-gray-800 px-3 py-2.5 text-sm text-gray-100 placeholder-gray-500 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30"
          aria-required="true"
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="password" className="text-sm font-medium text-gray-300">
          密码
        </label>
        <input
          id="password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="请输入密码"
          className="rounded-lg border border-gray-700 bg-gray-800 px-3 py-2.5 text-sm text-gray-100 placeholder-gray-500 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30"
          aria-required="true"
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="captchaCode" className="text-sm font-medium text-gray-300">
          验证码
        </label>
        <div className="flex gap-2">
          <input
            id="captchaCode"
            type="text"
            autoComplete="off"
            required
            value={captchaCode}
            onChange={(e) => setCaptchaCode(e.target.value)}
            placeholder="请输入验证码"
            className="flex-1 rounded-lg border border-gray-700 bg-gray-800 px-3 py-2.5 text-sm text-gray-100 placeholder-gray-500 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30"
            aria-required="true"
          />
          <button
            type="button"
            onClick={refreshCaptcha}
            aria-label="刷新验证码"
            className="w-28 shrink-0 overflow-hidden rounded-lg border border-gray-700 bg-gray-800 transition hover:border-gray-500"
          >
            {captchaUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={captchaUrl}
                alt="验证码"
                className="h-full w-full object-cover"
              />
            ) : (
              <span className="flex h-full w-full items-center justify-center text-xs text-gray-500">
                加载中…
              </span>
            )}
          </button>
        </div>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="mt-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 focus:ring-offset-gray-900 disabled:cursor-not-allowed disabled:opacity-50"
        aria-busy={loading}
      >
        {loading ? "登录中…" : "登录"}
      </button>
    </form>
  );
}
