import { useMemo, useState, type ReactNode } from "react";
import { AuthContext, type AuthContextType } from "./AuthContext";
import type { User } from "./AuthContext";

interface ProviderProps {
    children: ReactNode;
}

export const AuthProvider = ({children}: ProviderProps) => {
    const [user, setUser] = useState<User|undefined>(undefined);

    const logout = () => {};

    const authValue = useMemo<AuthContextType>(() => ({
        user,
        setUser,
        logout
    }), [user,setUser]);

    return (
        <AuthContext value={authValue}>
            {children}
        </AuthContext>
    );
};