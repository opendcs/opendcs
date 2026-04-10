import type { User } from "opendcs-api";
import { createContext, useContext, type SetStateAction } from "react";
import type { Scheme } from "../../util/login-providers/Scheme.types";

export interface AuthContextType {
  user?: User;
  isLoading: boolean;
  loginSchemes: Record<string, Scheme>;
  setUser: React.Dispatch<React.SetStateAction<User | undefined>>;
  setSchemes: (schemes: Record<string, Scheme>) => void;
  logout: () => void;
}

const defaultValue: AuthContextType = {
  user: undefined,
  isLoading: true,
  loginSchemes: {},
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  setUser: (_value: SetStateAction<User | undefined>) => {},
  setSchemes: () => {},
  logout: () => {},
};

export const AuthContext = createContext<AuthContextType>(defaultValue);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context == undefined) {
    throw new Error("Auth isn't defined?");
  }
  return context;
};
