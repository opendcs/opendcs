import { OidcClient } from "oidc-client-ts";
import type { OidcScheme, Scheme } from "./Scheme.types";

export async function fromOpenApiUrl(url: URL): Promise<Record<string, Scheme>> {
  const res = await fetch(url);
  if (res.ok) {
    const data = await res.json();
    return fromOpenApiData(data);
  }
  return {};
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function fromOpenApiData(
  spec: Record<string, any>,
): Promise<Record<string, Scheme>> {
  let schemes: Record<string, Scheme> = {};
  if (spec.components != null) {
    const apiSchemes = spec.components.securitySchemes!;
    for (const scheme in apiSchemes) {
      let option: Scheme = apiSchemes[scheme]["x-logincomponent-configuration"];
      console.log(option);
      console.log(apiSchemes[scheme]);
      console.log(apiSchemes[scheme].openIdConnectUrl);
      if (apiSchemes[scheme].openIdConnectUrl) {
        option = {
          oidcConfig: {
            ...option.oidcConfig,
            wellKnownUrl: apiSchemes[scheme].openIdConnectUrl,
          },
        };
      }
      console.log(option);
      schemes = {
        ...schemes,
        [scheme]: option,
      };
    }
  }
  return schemes;
}

/**
 * Helper to build Oidc TS clients from Scheme configurations
 * @param scheme OidcScheme to map to a client.
 * @returns
 */
export const oidcConfigToClient = (scheme: OidcScheme): OidcClient => {
  return new OidcClient({
    redirect_uri: scheme.oidcConfig.redirectUri,
    client_id: scheme.oidcConfig.clientId,
    authority: scheme.oidcConfig.wellKnownUrl.replace(/\/\.well-known.*$/, ""),
    disablePKCE: !scheme.oidcConfig.usePkce,
  });
};
