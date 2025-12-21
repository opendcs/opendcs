import { useState, type ReactNode } from "react";
import { AuthContext, type AuthContextType, type User } from "./AuthContext";
import { useApi } from "./ApiContext";
import { RESTAuthenticationAndAuthorizationApi } from 'opendcs-api';

interface ProviderProps {
    children: ReactNode;
}

export const AuthProvider = ({children}: ProviderProps) => {
    const [user, setUser] = useState<User|undefined>(undefined);

    const apiContext = useApi();

    const logout = () => {
        const auth = new RESTAuthenticationAndAuthorizationApi(apiContext.conf);
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        auth.logout().then((_value: void) => setUser(undefined));
    };

    const authValue: AuthContextType = {
        user,
        setUser,
        logout
    };

    return (
        <AuthContext value={authValue}>
            {children}
        </AuthContext>
    );
};