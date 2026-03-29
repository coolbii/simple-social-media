import { defineStore } from 'pinia';
import { getCurrentUser, login, logout, register } from '../services/api';
import type { AuthStatus, UserSummary } from '../types';

interface AuthState {
  status: AuthStatus;
  currentUser: UserSummary | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    status: 'checking',
    currentUser: null,
  }),
  getters: {
    isAuthenticated: (state) => state.status === 'authenticated',
  },
  actions: {
    async bootstrap() {
      this.status = 'checking';
      try {
        this.currentUser = await getCurrentUser();
        this.status = 'authenticated';
      } catch {
        this.currentUser = null;
        this.status = 'anonymous';
      }
    },
    async signIn(phoneNumber: string, password: string) {
      const response = await login({ phoneNumber, password });
      this.currentUser = response.user;
      this.status = 'authenticated';
      return response.user;
    },
    async signUp(payload: { phoneNumber: string; userName: string; password: string; email?: string }) {
      return register(payload);
    },
    async signOut() {
      try {
        await logout();
      } finally {
        this.currentUser = null;
        this.status = 'anonymous';
      }
    },
  },
});
