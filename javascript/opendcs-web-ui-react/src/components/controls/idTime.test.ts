import { describe, it, expect } from "vitest";
import {
  dateToInputValue,
  dayOfYear,
  formatAbsolute,
  inputValueToDate,
  isNow,
  isRelative,
  parseAbsolute,
  parseNowMinus,
} from "./idTime";

describe("isRelative / isNow", () => {
  it("detects relative strings", () => {
    expect(isRelative("now")).toBe(true);
    expect(isRelative("now - 1 hour")).toBe(true);
    expect(isRelative("  NOW - 2 days")).toBe(true);
    expect(isRelative("2024/001 00:00:00")).toBe(false);
    expect(isRelative(undefined)).toBe(false);
  });

  it("detects exact now", () => {
    expect(isNow("now")).toBe(true);
    expect(isNow("  NOW ")).toBe(true);
    expect(isNow("now - 1 hour")).toBe(false);
  });
});

describe("parseNowMinus", () => {
  it("extracts the amount", () => {
    expect(parseNowMinus("now - 1 hour")).toBe("1 hour");
    expect(parseNowMinus("now-30 minutes")).toBe("30 minutes");
    expect(parseNowMinus("NOW -  2 days")).toBe("2 days");
  });

  it("returns undefined for missing/invalid amounts", () => {
    expect(parseNowMinus("now - null")).toBeUndefined();
    expect(parseNowMinus("now -")).toBeUndefined();
    expect(parseNowMinus("now")).toBeUndefined();
    expect(parseNowMinus("2024/001 00:00:00")).toBeUndefined();
  });
});

describe("dayOfYear / formatAbsolute / parseAbsolute", () => {
  it("computes day of year in UTC", () => {
    expect(dayOfYear(new Date(Date.UTC(2024, 0, 1, 12, 0, 0)))).toBe(1);
    expect(dayOfYear(new Date(Date.UTC(2024, 1, 1, 0, 0, 0)))).toBe(32);
    expect(dayOfYear(new Date(Date.UTC(2024, 11, 31, 23, 59, 59)))).toBe(366); // leap
  });

  it("formats absolute strings as YYYY/DDD HH:MM:SS in GMT", () => {
    expect(formatAbsolute(new Date(Date.UTC(2026, 4, 28, 18, 8, 19)))).toBe(
      "2026/148 18:08:19",
    );
    expect(formatAbsolute(new Date(Date.UTC(2024, 0, 1, 0, 0, 0)))).toBe(
      "2024/001 00:00:00",
    );
  });

  it("round-trips absolute strings", () => {
    const original = new Date(Date.UTC(2025, 6, 15, 9, 30, 45));
    const parsed = parseAbsolute(formatAbsolute(original));
    expect(parsed?.getTime()).toBe(original.getTime());
  });

  it("parses without seconds and rejects junk", () => {
    expect(parseAbsolute("2024/032 06:15")?.getTime()).toBe(
      Date.UTC(2024, 1, 1, 6, 15, 0),
    );
    expect(parseAbsolute("now - 1 hour")).toBeNull();
    expect(parseAbsolute("")).toBeNull();
  });
});

describe("datetime-local conversions", () => {
  it("round-trips through input value (UTC)", () => {
    const d = new Date(Date.UTC(2026, 4, 28, 18, 8, 0));
    expect(dateToInputValue(d)).toBe("2026-05-28T18:08");
    expect(inputValueToDate("2026-05-28T18:08")?.getTime()).toBe(d.getTime());
  });

  it("rejects blank/invalid input", () => {
    expect(inputValueToDate("")).toBeNull();
    expect(inputValueToDate("not-a-date")).toBeNull();
  });
});
