import { createContext } from "react";

export interface AuthContextType {
    user: string,
    setUser: (name: string) => void
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);