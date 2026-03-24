import { OidcClient } from "oidc-client-ts";
import { useApi } from "../../../contexts/app/ApiContext";
import { RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { useAuth } from "../../../contexts/app/AuthContext";
import { useLocation, useNavigate } from "react-router-dom";
import type { OidcScheme } from "../../../util/login-providers/Scheme.types";
import { oidcConfigToClient } from "../../../util/login-providers";

export const OidcCallback: React.FC = () => {
  const api = useApi();
  const { setUser, loginSchemes } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // pull the saved state so we can match it to the right configuration.
  const url = new URL(globalThis.location.href);
  const state: string = url.searchParams.get("state") || "";
  const clientId = localStorage.getItem(state);

  let client: OidcClient | null = null;
  for (const schemeKey in loginSchemes) {
    const scheme = loginSchemes[schemeKey];
    if (scheme.oidcConfig?.clientId === clientId) {
      const oidcScheme = scheme as OidcScheme;
      client = oidcConfigToClient(oidcScheme);
    }
  }

  if (client === null) {
    return <span>No valid configuration to handle request.</span>;
  }

  client.processSigninResponse(globalThis.location.href).then((r) => {
    console.log(r);
    const login = new RESTAuthenticationAndAuthorizationApi(api.conf);
    login
      // office doesn't matter for initial login
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

  return <span>Signin Proceedure failed.</span>;
};

export default OidcCallback;
