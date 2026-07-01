import { create } from "zustand";
import type { LoginResponse } from "./authApi";

interface AuthState {
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

export const useAuth = create<AuthState>((set, get) => ({
  accountId: null,
  nickname: null,
  accountType: null,
  tenantId: null,
  tenantName: null,
  expiresAt: null,

  setAuth(response: LoginResponse) {
    set({
      accountId: response.accountId,
      nickname: response.nickname,
      accountType: response.accountType,
      tenantId: response.tenantId,
      tenantName: response.tenantName,
      expiresAt: response.expiresAt,
    });
  },

  clearAuth() {
    set({
      accountId: null,
      nickname: null,
      accountType: null,
      tenantId: null,
      tenantName: null,
      expiresAt: null,
    });
  },

  isAuthenticated() {
    const { accountId } = get();
    return accountId !== null && !get().isExpired();
  },

  isExpired() {
    const { expiresAt } = get();
    if (!expiresAt) return true;
    return new Date(expiresAt).getTime() < Date.now();
  },
}));
