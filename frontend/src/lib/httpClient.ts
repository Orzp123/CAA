import axios, { type AxiosInstance } from "axios";

export function createAuthenticatedClient(baseURL = "/api"): AxiosInstance {
  const client = axios.create({
    baseURL,
    headers: { "Content-Type": "application/json" },
    timeout: 30000,
    withCredentials: true,
  });

  client.interceptors.response.use(
    (res) => res,
    (err) => {
      if (err.response?.status === 401 && typeof window !== "undefined") {
        sessionStorage.removeItem("caa_auth");
        // fire-and-forget：通知服务端清理 token（忽略失败，避免循环）
        fetch("/api/v1/auth/logout", { method: "POST", credentials: "include" }).catch(() => {});
        window.location.href = "/login";
      }
      return Promise.reject(err);
    }
  );

  return client;
}
