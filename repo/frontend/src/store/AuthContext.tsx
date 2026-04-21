import { createContext, useCallback, useContext, useMemo, useState, ReactNode } from 'react';
import type { AuthUser } from '../types';
import { apiPost } from '../api/client';

interface AuthContextValue {
  user: AuthUser | null;
  login: (username: string, password: string) => Promise<AuthUser>;
  register: (username: string, password: string, displayName: string) => Promise<AuthUser>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function loadUser(): AuthUser | null {
  const raw = localStorage.getItem('citybus_user');
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadUser());

  const persist = (u: AuthUser) => {
    localStorage.setItem('citybus_token', u.token);
    localStorage.setItem('citybus_user', JSON.stringify(u));
    setUser(u);
  };

  const login = useCallback(async (username: string, password: string) => {
    const resp = await apiPost<AuthUser>('/api/auth/login', { username, password });
    persist(resp);
    return resp;
  }, []);

  const register = useCallback(async (username: string, password: string, displayName: string) => {
    const resp = await apiPost<AuthUser>('/api/auth/register', { username, password, displayName });
    persist(resp);
    return resp;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('citybus_token');
    localStorage.removeItem('citybus_user');
    setUser(null);
  }, []);

  const value = useMemo(() => ({ user, login, register, logout }), [user, login, register, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
