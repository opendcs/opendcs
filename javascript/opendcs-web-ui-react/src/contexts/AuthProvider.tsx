import { useState, type ReactNode } from "react";
import { AuthContext, type AuthContextType } from "./AuthContext";
import { useApi } from "./ApiContext";
import { RESTAuthenticationAndAuthorizationApi, User } from 'opendcs-api';
import { useNavigate } from "react-router-dom";

interface ProviderProps {
    children: ReactNode;
}

export const AuthProvider = ({children}: ProviderProps) => {
    const navigate = useNavigate();
    const [user, setUser] = useState<User|undefined>(undefined);

    const apiContext = useApi();

    const logout = () => {
        const auth = new RESTAuthenticationAndAuthorizationApi(apiContext.conf);
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        auth.logout().then((_value: void) => {
            setUser(undefined);
            navigate("/login");
        });
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