import { OidcClient } from "oidc-client-ts";
import { useApi } from "../../../contexts/app/ApiContext";
import { RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { useAuth } from "../../../contexts/app/AuthContext";
import { useLocation, useNavigate } from "react-router-dom";

export const OidcCallback: React.FC = () => {
  const api = useApi();
  const { setUser } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const client = new OidcClient({
    redirect_uri: "http://localhost:5173/oidc-callback",
    client_id: "opendcs-public",
    authority: "http://localhost:7100/auth/realms/opendcs",
  });

  client.processSigninResponse(window.location.href).then((r) => {
    console.log(r);
    const login = new RESTAuthenticationAndAuthorizationApi(api.conf);
    login
      .postJwt("", "Bearer " + r.access_token)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      .then((user_value: any) => {
        setUser(user_value);
        const redirectPath = location.state?.from || "/platforms";
        navigate(redirectPath, { replace: true });
      })
      .catch((error_: { toString: () => string }) => {
        // TODO: login error modal
        console.log(error_.toString());
        //setShowErrorModal(true);
      });
  });

  return <span>Hello?</span>;
};

export default OidcCallback;
