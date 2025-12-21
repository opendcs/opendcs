import { createContext, useContext } from "react";

export type User = {
    username?: string
}

export interface AuthContextType {
    user: User,
    setUser: React.Dispatch<React.SetStateAction<User>>
}

export const AuthContext = createContext<AuthContextType|undefined>(undefined);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context == undefined)
    {
        throw new Error("Auth isn't defined?");
    }
    return context;
}
