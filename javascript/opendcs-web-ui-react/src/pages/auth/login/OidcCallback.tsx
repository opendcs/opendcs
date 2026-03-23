import { OidcClient } from "oidc-client-ts";
import { useApi } from "../../../contexts/app/ApiContext";
import { RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { useAuth } from "../../../contexts/app/AuthContext";
import { useLocation, useNavigate } from "react-router-dom";

export const OidcCallback: React.FC = () => {
  const api = useApi();
  const { setUser, loginSchemes } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  console.log(window.location.href);
  const url = new URL(window.location.href);
  const state = url.searchParams.get("state") as string;
  console.log(state);
  console.log(atob(state));
  const clientId = localStorage.getItem(state);
  console.log(clientId);

  let client: OidcClient | null = null;
  for (const schemeKey in loginSchemes) {
    const scheme = loginSchemes[schemeKey];
    if (scheme.oidcConfig?.clientId === clientId) {
      client = new OidcClient({
        redirect_uri: scheme.oidcConfig.redirectUri as string,
        client_id: clientId,
        authority: "http://localhost:7100/auth/realms/opendcs",
      });
    }
  }

  if (client === null) {
    return <span>No valid configuration to handle request.</span>;
  }

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
