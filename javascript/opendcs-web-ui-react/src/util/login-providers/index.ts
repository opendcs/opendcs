import type { Scheme } from "./Scheme.types";

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
      const option: Scheme = apiSchemes[scheme]["x-logincomponent-configuration"];
      schemes = {
        ...schemes,
        [scheme]: option,
      };
    }
  }
  return schemes;
}
