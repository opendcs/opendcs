import { useEffect, useState, type ReactNode } from "react";
import { AuthContext, type AuthContextType } from "./AuthContext";
import { useApi } from "./ApiContext";
import { RESTAuthenticationAndAuthorizationApi, User } from "opendcs-api";
import { useNavigate } from "react-router-dom";

interface ProviderProps {
  children: ReactNode;
}

export const AuthProvider = ({ children }: ProviderProps) => {
  const navigate = useNavigate();
  const [user, setUser] = useState<User | null | undefined>(undefined);

  const apiContext = useApi();

  useEffect(() => {
    const auth = new RESTAuthenticationAndAuthorizationApi(apiContext.conf);
    auth
      .checkSessionAuthorization()
      .then((value: User) => setUser(value))
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      .catch((_error: unknown) => setUser(null));
  }, [apiContext.conf]);

  const logout = () => {
    const auth = new RESTAuthenticationAndAuthorizationApi(apiContext.conf);
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    auth.logout().then((_value: void) => {
      setUser(null);
      navigate("/login");
    });
  };

  const authValue: AuthContextType = {
    user,
    isLoading: user === undefined,
    setUser,
    logout,
  };

  return <AuthContext value={authValue}>{children}</AuthContext>;
};
