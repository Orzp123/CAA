import axios from "axios";

const api = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
  timeout: 30000,
});

api.interceptors.request.use((config) => {
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && typeof window !== "undefined") {
      localStorage.removeItem("token");
      window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);

// Agents
export const agentApi = {
  list: (activeOnly = false) => api.get("/agents", { params: { activeOnly } }),
  get: (id: string) => api.get(`/agents/${id}`),
  create: (data: unknown) => api.post("/agents", data),
  update: (id: string, data: unknown) => api.put(`/agents/${id}`, data),
  delete: (id: string) => api.delete(`/agents/${id}`),
};

// Workflows
export const workflowApi = {
  start: (id: string, input?: Record<string, unknown>) =>
    api.post(`/workflows/${id}/start`, input ?? {}),
  status: (workflowId: string) => api.get(`/workflows/${workflowId}/status`),
  terminate: (workflowId: string, reason?: string) =>
    api.delete(`/workflows/${workflowId}`, { params: { reason } }),
};

// Pages
export const pageApi = {
  list: () => api.get("/pages"),
  get: (id: string) => api.get(`/pages/${id}`),
  getByPath: (path: string) => api.get("/pages/by-path", { params: { path } }),
  create: (data: unknown) => api.post("/pages", data),
  update: (id: string, data: unknown) => api.put(`/pages/${id}`, data),
  delete: (id: string) => api.delete(`/pages/${id}`),
};

// Chat
export const chatApi = {
  send: (agentId: string, message: string, history: unknown[]) =>
    api.post(`/chat/${agentId}`, { message, history }),
  streamUrl: (agentId: string) => `/api/chat/${agentId}/stream`,
};

export default api;
