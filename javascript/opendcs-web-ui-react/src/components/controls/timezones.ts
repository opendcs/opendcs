// Time zone names offered by TimezoneSelect. Kept free of React so the list
// rules can be unit tested.

// Used when the runtime has no Intl.supportedValuesOf.
const FALLBACK_TIME_ZONES: readonly string[] = [
  "America/New_York",
  "America/Chicago",
  "America/Denver",
  "America/Los_Angeles",
  "America/Anchorage",
  "Pacific/Honolulu",
];

// V8 (Chrome, Edge) leaves UTC and GMT out of supportedValuesOf, so they are
// added back in alphabetical order.
const EXTRA_TIME_ZONES: readonly string[] = ["UTC", "GMT"];

export type TimeZoneSource = () => readonly string[];

const intlTimeZones: TimeZoneSource = () => {
  const intl = Intl as unknown as { supportedValuesOf?: (k: string) => string[] };
  return intl.supportedValuesOf?.("timeZone") ?? FALLBACK_TIME_ZONES;
};

const byName = (a: string, b: string): number => a.localeCompare(b);

/** Every zone the runtime knows, plus UTC and GMT, each once and sorted. */
export const getTimeZones = (source: TimeZoneSource = intlTimeZones): string[] => {
  let zones = FALLBACK_TIME_ZONES;
  try {
    zones = source();
  } catch {
    // fall through to fallback list
  }
  return [...new Set([...zones, ...EXTRA_TIME_ZONES])].sort(byName);
};

/**
 * Keeps a saved zone that isn't in the list (an alias such as "US/Eastern",
 * or "CST") selectable, sorted into place rather than pinned to the top.
 */
export const withTimeZone = (
  zones: readonly string[],
  value: string | undefined,
): readonly string[] =>
  value && !zones.includes(value) ? [...zones, value].sort(byName) : zones;
