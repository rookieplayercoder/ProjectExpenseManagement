import * as React from "react";
import { authApi } from "@/api/authApi";
import { userApi } from "@/api/userApi";
import { authStorage } from "@/utils/authStorage";
import type { AuthUser, CreateUserRequest, LoginRequest } from "@/types/auth";

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  login: (payload: LoginRequest) => Promise<void>;
  register: (payload: CreateUserRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = React.createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = React.useState<AuthUser | null>(null);
  const [isInitializing, setIsInitializing] = React.useState(true);

  // Rehydrate session from localStorage on first load.
  React.useEffect(() => {
    const token = authStorage.getToken();
    const storedUser = authStorage.getUser();
    if (token && storedUser) {
      setUser(storedUser);
    }
    setIsInitializing(false);
  }, []);

  const login = React.useCallback(async (payload: LoginRequest) => {
    const response = await authApi.login(payload);
    const authUser: AuthUser = {
      userId: response.userId,
      email: response.email,
      role: response.role,
    };
    authStorage.setToken(response.accessToken);
    authStorage.setUser(authUser);
    setUser(authUser);
  }, []);

  // Registration only creates the account - the backend has no auto-login
  // response for it, so we register then let the caller redirect to /login.
  const register = React.useCallback(async (payload: CreateUserRequest) => {
    await userApi.register(payload);
  }, []);

  const logout = React.useCallback(() => {
    authStorage.clear();
    setUser(null);
  }, []);

  const value: AuthContextValue = {
    user,
    isAuthenticated: !!user,
    isInitializing,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuthContext(): AuthContextValue {
  const ctx = React.useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuthContext must be used within an AuthProvider");
  }
  return ctx;
}
