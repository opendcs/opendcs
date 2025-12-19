import { createContext } from "react";

export type User = {
    username?: string
}

export interface AuthContextType {
    user?: User,
    setUser: React.Dispatch<React.SetStateAction<User>>
}

export const AuthContext = createContext<AuthContextType|undefined>(undefined);