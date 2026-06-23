import { create } from "zustand";
import type { LoginResponse } from "./authApi";

const TOKEN_KEY = "caa_token";

interface AuthState {
  token: string | null;
  accountId: string | null;
  nickname: string | null;
  accountType: string | null;
  tenantId: string | null;
  tenantName: string | null;
  expiresAt: string | null;

  setAuth: (response: LoginResponse) => void;
  clearAuth: () => void;
  isAuthenticated: () => boolean;
  isExpired: () => boolean;
}

type StoredAuth = {
  token?: string | null;
  accountId?: string | null;
  nickname?: string | null;
  accountType?: string | null;
  tenantId?: string | null;
  tenantName?: string | null;
  expiresAt?: string | null;
};

function loadFromStorage(): StoredAuth {
  if (typeof window === "undefined") return {};
  try {
    const raw = localStorage.getItem(TOKEN_KEY);
    if (!raw) return {};
    return JSON.parse(raw) as StoredAuth;
  } catch {
    return {};
  }
}

function saveToStorage(state: Omit<AuthState, "setAuth" | "clearAuth" | "isAuthenticated" | "isExpired">): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(TOKEN_KEY, JSON.stringify(state));
}

function clearStorage(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(TOKEN_KEY);
}

const persisted = loadFromStorage();

export const useAuth = create<AuthState>((set, get) => ({
  token: persisted.token ?? null,
  accountId: persisted.accountId ?? null,
  nickname: persisted.nickname ?? null,
  accountType: persisted.accountType ?? null,
  tenantId: persisted.tenantId ?? null,
  tenantName: persisted.tenantName ?? null,
  expiresAt: persisted.expiresAt ?? null,

  setAuth(response: LoginResponse) {
    const next = {
      token: response.token,
      accountId: response.accountId,
      nickname: response.nickname,
      accountType: response.accountType,
      tenantId: response.tenantId,
      tenantName: response.tenantName,
      expiresAt: response.expiresAt,
    };
    saveToStorage(next);
    set(next);
  },

  clearAuth() {
    clearStorage();
    set({
      token: null,
      accountId: null,
      nickname: null,
      accountType: null,
      tenantId: null,
      tenantName: null,
      expiresAt: null,
    });
  },

  isAuthenticated() {
    const { token } = get();
    return token !== null && !get().isExpired();
  },

  isExpired() {
    const { expiresAt } = get();
    if (!expiresAt) return true;
    return new Date(expiresAt).getTime() < Date.now();
  },
}));
