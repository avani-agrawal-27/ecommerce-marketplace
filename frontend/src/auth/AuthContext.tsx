import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

import {
  login as loginApi,
  register as registerApi,
  getCurrentUser,
} from "../api/authApi";

import type { LoginRequest, RegisterRequest, User } from "../types/auth";

import { getToken, removeToken, saveToken } from "./tokenStorage";

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const restoreAuthentication = async () => {
      const token = getToken();

      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const currentUser = await getCurrentUser();
        setUser(currentUser);
      } catch (error) {
        // removeToken();
        console.error("AUTH RESTORE FAILED:", error);
        console.error("TOKEN STILL EXISTS:", getToken());

        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    restoreAuthentication();
  }, []);

  const login = async (request: LoginRequest): Promise<void> => {
    const response = await loginApi(request);

    saveToken(response.accessToken);
    setUser(response.user);
  };

  const register = async (request: RegisterRequest): Promise<void> => {
    const response = await registerApi(request);

    saveToken(response.accessToken);
    setUser(response.user);
  };

  const logout = (): void => {
    removeToken();
    setUser(null);
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: user !== null,
    loading,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }

  return context;
};
