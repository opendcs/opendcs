import { useState, type ReactNode } from "react";
import { AuthContext } from "./AuthContext";
import type { User } from "./AuthContext";

interface ProviderProps {
    children: ReactNode;
}

export const AuthProvider = ({children}: ProviderProps) => {
    const [user, setUser] = useState<User>({username: undefined});

    return (
        <AuthContext value={{user, setUser}}>
            {children}
        </AuthContext>
    );
};