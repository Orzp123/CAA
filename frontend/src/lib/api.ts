import { createAuthenticatedClient } from "./httpClient";

const api = createAuthenticatedClient();

// ── Types ──────────────────────────────────────────────────────────────────

export interface Agent {
  id: string;
  name: string;
  description: string;
  provider: string;
  model: string;
  status: string;
}

export interface WorkflowStatus {
  workflowId: string;
  status: string;
  result?: unknown;
}

export interface PageSchema {
  id: string;
  path: string;
  schema: unknown;
}

export interface ChatResponse {
  content: string;
}

// ── Agents ─────────────────────────────────────────────────────────────────

export const agentApi = {
  list: (activeOnly = false) =>
    api.get<Agent[]>("/agents", { params: { activeOnly } }),
  get: (id: string) =>
    api.get<Agent>(`/agents/${id}`),
  create: (data: unknown) =>
    api.post<Agent>("/agents", data),
  update: (id: string, data: unknown) =>
    api.put<Agent>(`/agents/${id}`, data),
  delete: (id: string) =>
    api.delete<void>(`/agents/${id}`),
};

// ── Workflows ──────────────────────────────────────────────────────────────

export const workflowApi = {
  start: (id: string, input?: Record<string, unknown>) =>
    api.post<WorkflowStatus>(`/workflows/${id}/start`, input ?? {}),
  status: (workflowId: string) =>
    api.get<WorkflowStatus>(`/workflows/${workflowId}/status`),
  terminate: (workflowId: string, reason?: string) =>
    api.delete<void>(`/workflows/${workflowId}`, { params: { reason } }),
};

// ── Pages ──────────────────────────────────────────────────────────────────

export const pageApi = {
  list: () =>
    api.get<PageSchema[]>("/pages"),
  get: (id: string) =>
    api.get<PageSchema>(`/pages/${id}`),
  getByPath: (path: string) =>
    api.get<PageSchema>("/pages/by-path", { params: { path } }),
  create: (data: unknown) =>
    api.post<PageSchema>("/pages", data),
  update: (id: string, data: unknown) =>
    api.put<PageSchema>(`/pages/${id}`, data),
  delete: (id: string) =>
    api.delete<void>(`/pages/${id}`),
};

// ── Chat ───────────────────────────────────────────────────────────────────

export const chatApi = {
  send: (agentId: string, message: string, history: unknown[]) =>
    api.post<ChatResponse>(`/chat/${agentId}`, { message, history }),
  streamUrl: (agentId: string) => `/api/chat/${agentId}/stream`,
};

export default api;
