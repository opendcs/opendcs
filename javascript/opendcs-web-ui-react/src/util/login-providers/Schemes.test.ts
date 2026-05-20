import { expect, test } from "vitest";
import { fromOpenApiData } from ".";
import { readFile } from "node:fs/promises";
import type { FormScheme, OidcScheme, Scheme } from "./Scheme.types";

test("can process schemes", async () => {
  const apiString: string = await readFile(
    `.storybook/mock/openapi-security-schemes.json`,
    { encoding: "utf8" },
  );
  const apiData: object = JSON.parse(apiString);

  console.log(apiData);

  const schemes: Record<string, Scheme> = await fromOpenApiData(apiData);
  console.log(schemes);
  //expect(schemes.length).toBe(3)

  const scheme = schemes.builttin as FormScheme;
  expect(scheme.formConfig).not.toBeNull();
  expect(scheme.formConfig.usernameInput).toBe("username");
  expect(scheme.formConfig.passwordInput).toBe("password");
  expect(scheme.queryParameters.login).toBe("prompt");

  const oidcScheme = schemes["oidc-pkce"] as OidcScheme;
  expect(oidcScheme.oidcConfig).not.toBeNull();
  expect(oidcScheme.oidcConfig.clientId).toBe("opendcs-public");
  expect(oidcScheme.queryParameters.kc_idp_hint).toStrictEqual([
    "federated-provider-1",
    "federated-provider-2",
  ]);
});
