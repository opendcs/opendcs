import type { ApiNetListItem, ApiPlatformRef } from "opendcs-api";

/**
 * A platform that can be added to a network list, flattened together with the
 * netlist item it would produce so the chooser can search and sort on those
 * values directly.
 */
export interface PlatformCandidate {
  platformId: number;
  /** The platform's display name (site name plus designator). */
  platform: string;
  transportId: string;
  /** The name stored on the netlist item. */
  platformName: string;
  agency: string;
  config: string;
  description: string;
}

const isGoes = (type: string) => type.startsWith("goes");

/**
 * The platform's medium id for the netlist's transport medium type. A ref's
 * `transportMedia` maps medium type to medium id (the REST mapper also mixes in
 * platform properties, so only matching keys are used). Like the desktop
 * editor, GOES lists accept any GOES medium, preferring an exact type match.
 */
export function transportIdFor(
  platform: ApiPlatformRef,
  mediumType: string | undefined,
): string | undefined {
  const wanted = mediumType?.trim().toLowerCase();
  if (!wanted) return undefined;
  const media = Object.entries(platform.transportMedia ?? {})
    .map(([type, id]) => [type.toLowerCase(), id?.trim() ?? ""] as const)
    .filter(([, id]) => id !== "");
  const exact = media.find(([type]) => type === wanted);
  if (exact) return exact[1];
  if (isGoes(wanted)) return media.find(([type]) => isGoes(type))?.[1];
  return undefined;
}

/**
 * The site name of the list's preferred name type, falling back to the
 * platform's display name when the site has no name of that type.
 */
export function platformNameFor(
  platform: ApiPlatformRef,
  siteNameTypePref: string | undefined,
): string {
  const pref = siteNameTypePref?.trim().toLowerCase();
  if (pref) {
    const match = Object.entries(platform.sitenames ?? {}).find(
      ([type]) => type.toLowerCase() === pref,
    );
    if (match?.[1]) return match[1];
  }
  return platform.name ?? "";
}

/** Platforms that have a medium of the netlist's type, as netlist candidates. */
export function platformCandidates(
  platforms: ApiPlatformRef[],
  mediumType: string | undefined,
  siteNameTypePref: string | undefined,
): PlatformCandidate[] {
  const out: PlatformCandidate[] = [];
  for (const p of platforms) {
    const transportId = transportIdFor(p, mediumType);
    if (!transportId || p.platformId === undefined) continue;
    out.push({
      platformId: p.platformId,
      platform: p.name ?? "",
      transportId,
      platformName: platformNameFor(p, siteNameTypePref),
      agency: p.agency ?? "",
      config: p.config ?? "",
      description: p.description ?? "",
    });
  }
  return out;
}

export const toNetlistItem = (c: PlatformCandidate): ApiNetListItem => ({
  transportId: c.transportId,
  platformName: c.platformName,
  description: c.description,
});
