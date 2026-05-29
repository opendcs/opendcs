// Helpers for OpenDCS routing-spec since/until time strings, mirroring
// ilex.util.IDateFormat in the Java codebase.
//
// Relative strings start with "now" (e.g. "now", "now - 1 hour").
// Absolute strings use the OpenDCS day-of-year form "YYYY/DDD HH:MM:SS" in GMT
// (matching IDateFormat.toString(date, false)).

const ONE_DAY_MS = 86_400_000;

const pad2 = (n: number): string => String(n).padStart(2, "0");
const pad3 = (n: number): string => String(n).padStart(3, "0");

/** True when the string is relative to "now" (case-insensitive). */
export const isRelative = (s: string | undefined | null): boolean =>
  !!s && s.trim().toLowerCase().startsWith("now");

/** True when the trimmed string is exactly "now". */
export const isNow = (s: string | undefined | null): boolean =>
  !!s && s.trim().toLowerCase() === "now";

/**
 * Extracts the amount from a "now - <amount>" string. Returns undefined when the
 * string isn't a now-minus expression or the amount is missing/invalid (e.g. the
 * buggy "now - null").
 */
export const parseNowMinus = (s: string | undefined | null): string | undefined => {
  if (!s) return undefined;
  const trimmed = s.trim();
  if (!trimmed.toLowerCase().startsWith("now")) return undefined;
  const afterNow = trimmed.slice(3).trim();
  if (!afterNow.startsWith("-")) return undefined;
  const amount = afterNow.slice(1).trim();
  if (!amount || amount.toLowerCase() === "null") return undefined;
  return amount;
};

/** Day of year (Jan 1 == 1) in UTC. */
export const dayOfYear = (d: Date): number => {
  const startOfYear = Date.UTC(d.getUTCFullYear(), 0, 1);
  const utcMidnight = Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate());
  return Math.round((utcMidnight - startOfYear) / ONE_DAY_MS) + 1;
};

/** Formats a Date as the OpenDCS absolute string "YYYY/DDD HH:MM:SS" (GMT). */
export const formatAbsolute = (d: Date): string => {
  const y = d.getUTCFullYear();
  return (
    `${y}/${pad3(dayOfYear(d))} ` +
    `${pad2(d.getUTCHours())}:${pad2(d.getUTCMinutes())}:${pad2(d.getUTCSeconds())}`
  );
};

/**
 * Parses an OpenDCS absolute string "YYYY/DDD HH:MM[:SS]" (GMT) into a Date.
 * Returns null when the string isn't in that form.
 */
export const parseAbsolute = (s: string | undefined | null): Date | null => {
  if (!s) return null;
  const m = /^(\d{4})\/(\d{1,3})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?$/.exec(s.trim());
  if (!m) return null;
  const [, year, doy, hh, mm, ss] = m;
  const ms =
    Date.UTC(Number(year), 0, 1) +
    (Number(doy) - 1) * ONE_DAY_MS +
    Number(hh) * 3_600_000 +
    Number(mm) * 60_000 +
    Number(ss ?? 0) * 1_000;
  const d = new Date(ms);
  return Number.isNaN(d.getTime()) ? null : d;
};

/** Converts a Date to the value of an <input type="datetime-local"> (UTC-based). */
export const dateToInputValue = (d: Date): string =>
  `${d.getUTCFullYear()}-${pad2(d.getUTCMonth() + 1)}-${pad2(d.getUTCDate())}` +
  `T${pad2(d.getUTCHours())}:${pad2(d.getUTCMinutes())}`;

/** Parses a datetime-local input value as UTC into a Date (null when blank/invalid). */
export const inputValueToDate = (v: string | undefined | null): Date | null => {
  if (!v) return null;
  const m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(v.trim());
  if (!m) return null;
  const [, y, mo, da, hh, mm, ss] = m;
  const d = new Date(
    Date.UTC(
      Number(y),
      Number(mo) - 1,
      Number(da),
      Number(hh),
      Number(mm),
      Number(ss ?? 0),
    ),
  );
  return Number.isNaN(d.getTime()) ? null : d;
};
