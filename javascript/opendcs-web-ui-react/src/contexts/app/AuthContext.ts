import type { User } from "opendcs-api";
import { createContext, useContext, type SetStateAction } from "react";

export interface AuthContextType {
  user?: User;
  setUser: React.Dispatch<React.SetStateAction<User | undefined>>;
  logout: () => void;
}

const defaultValue: AuthContextType = {
  user: undefined,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  setUser: (_value: SetStateAction<User | undefined>) => {},
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
