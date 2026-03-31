import { expect, test } from "vitest";
import { fromOpenApiData } from ".";
import { readFile } from "node:fs/promises";
import type { Scheme } from "./Scheme.types";

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

  const scheme = schemes.builttin;
  expect(scheme.formConfig).not.toBeNull();
  expect(scheme.formConfig.usernameInput).toBe("username");
  expect(scheme.formConfig.passwordInput).toBe("password");

  const oidcScheme = schemes["oidc-pkce"];
  expect(oidcScheme.oidcConfig).not.toBeNull();
  expect(oidcScheme.oidcConfig.clientId).toBe("opendcs-public");
});
