import type { SiteNameTypePreference } from "../../contexts/app/SiteNameTypeContext";

/**
 * @param sitenames map of name-type -> name value for the platform's site.
 * @param designator the platform's designator, if any.
 * @param preferredType the user's preferred enumerator, or "" for "no preference".
 * @param fallback value to use when no preference is set, or no name of the
 *   preferred (or any) type is available - normally the platform ref's
 *   server-computed `name` field.
 */
export const preferredPlatformName = (
  sitenames: { [key: string]: string } | undefined,
  designator: string | undefined,
  preferredType: SiteNameTypePreference,
  fallback: string | undefined,
): string | undefined => {
  if (!preferredType || !sitenames) return fallback;

  const entries = Object.entries(sitenames);
  if (entries.length === 0) return fallback;

  const match = entries.find(
    ([type]) => type.toLowerCase() === preferredType.toLowerCase(),
  );
  // Mirrors decodes.db.Site#getPreferredName(): fall back to the first
  // defined name if the preferred type isn't available for this site.
  const value = match?.[1] ?? entries[0][1];

  return designator ? `${value}-${designator}` : value;
};
