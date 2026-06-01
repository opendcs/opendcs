// Run-interval parsing for the schedule entry editor.
//
// The Swing dbeditor emits intervals as `<count> <unit>` strings, where unit
// is the singular form `minute`, `hour`, or `day`. `IntervalIncrement.parse`
// on the Java side accepts both singular and plural forms (and matches case-
// insensitively), so the parser here is correspondingly forgiving and
// serialization stays compatible with the legacy editor.

export type RunUnit = "minute" | "hour" | "day";

export interface RunIntervalParts {
  count: number;
  unit: RunUnit;
}

const UNIT_ALIASES: Record<string, RunUnit> = {
  minute: "minute",
  minutes: "minute",
  min: "minute",
  mins: "minute",
  hour: "hour",
  hours: "hour",
  hr: "hour",
  hrs: "hour",
  day: "day",
  days: "day",
};

export const parseRunInterval = (
  value: string | undefined,
): RunIntervalParts | null => {
  if (!value) return null;
  const trimmed = value.trim();
  if (!trimmed) return null;
  const match = /^(\d+)\s*([A-Za-z]+)$/.exec(trimmed);
  if (!match) return null;
  const count = Number(match[1]);
  if (!Number.isFinite(count) || count < 0) return null;
  const unit = UNIT_ALIASES[match[2].toLowerCase()];
  if (!unit) return null;
  return { count, unit };
};

export const formatRunInterval = (parts: RunIntervalParts): string =>
  `${parts.count} ${parts.unit}`;
