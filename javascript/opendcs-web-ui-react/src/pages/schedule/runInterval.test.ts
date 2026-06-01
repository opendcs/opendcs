import { describe, expect, test } from "vitest";
import { formatRunInterval, parseRunInterval } from "./runInterval";

// The Swing dbeditor emits `"<n> minute|hour|day"` (singular) but
// `IntervalIncrement.parse` accepts singular and plural forms — these tests
// pin that contract so the React component stays interoperable with records
// written by either editor.

describe("parseRunInterval", () => {
  test("accepts the Swing singular form", () => {
    expect(parseRunInterval("5 minute")).toEqual({ count: 5, unit: "minute" });
    expect(parseRunInterval("2 hour")).toEqual({ count: 2, unit: "hour" });
    expect(parseRunInterval("1 day")).toEqual({ count: 1, unit: "day" });
  });

  test("accepts the plural form the parser also produces", () => {
    expect(parseRunInterval("15 minutes")).toEqual({ count: 15, unit: "minute" });
    expect(parseRunInterval("3 hours")).toEqual({ count: 3, unit: "hour" });
    expect(parseRunInterval("7 days")).toEqual({ count: 7, unit: "day" });
  });

  test("is case-insensitive and tolerates extra whitespace", () => {
    expect(parseRunInterval("  10   HOURS  ")).toEqual({ count: 10, unit: "hour" });
    expect(parseRunInterval("4Min")).toEqual({ count: 4, unit: "minute" });
  });

  test("returns null for empty, junk, or unknown-unit values", () => {
    expect(parseRunInterval(undefined)).toBeNull();
    expect(parseRunInterval("")).toBeNull();
    expect(parseRunInterval("   ")).toBeNull();
    expect(parseRunInterval("hour")).toBeNull();
    expect(parseRunInterval("5 fortnights")).toBeNull();
    expect(parseRunInterval("-3 hour")).toBeNull();
  });
});

describe("formatRunInterval", () => {
  test("round-trips through parseRunInterval", () => {
    const original = "12 hours";
    const parsed = parseRunInterval(original);
    expect(parsed).not.toBeNull();
    if (!parsed) return;
    const formatted = formatRunInterval(parsed);
    expect(formatted).toBe("12 hour");
    // Parsing the formatted form yields the same structured value.
    expect(parseRunInterval(formatted)).toEqual(parsed);
  });
});
