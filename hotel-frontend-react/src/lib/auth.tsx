// Auth context — holds the normalised session user and exposes login/logout.
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { api } from "./api";
import {
  clearSession,
  getUser,
  isGroupAdmin as isGroupAdminUser,
  isHotelScoped as isHotelScopedUser,
  saveSession,
} from "./session";
import type { AnyLoginResponse, Role, StoredUser } from "./types";

interface AuthValue {
  user: StoredUser | null;
  isLoggedIn: boolean;
  isGroupAdmin: boolean;
  isHotelScoped: boolean;
  hasRole: (...roles: Role[]) => boolean;
  setSession: (res: AnyLoginResponse) => StoredUser;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<StoredUser | null>(() => getUser());

  const setSession = useCallback((res: AnyLoginResponse) => {
    const u = saveSession(res);
    setUser(u);
    return u;
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.auth.logout();
    } catch {
      // best-effort — clear locally regardless
    }
    clearSession();
    setUser(null);
  }, []);

  const value = useMemo<AuthValue>(
    () => ({
      user,
      isLoggedIn: !!user,
      isGroupAdmin: isGroupAdminUser(user),
      isHotelScoped: isHotelScopedUser(user),
      hasRole: (...roles: Role[]) => !!user && roles.includes(user.role),
      setSession,
      logout,
    }),
    [user, setSession, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
