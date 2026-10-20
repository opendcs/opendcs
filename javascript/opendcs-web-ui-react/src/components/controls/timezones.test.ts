import { describe, it, expect } from "vitest";
import { getTimeZones, withTimeZone } from "./timezones";

const sorted = (zones: readonly string[]): string[] =>
  [...zones].sort((a, b) => a.localeCompare(b));

// Well above the fallback list, well below the ~420 zones current engines report.
const EXPECTED_MIN_INTL_ZONES = 100;

describe("getTimeZones", () => {
  it("adds UTC and GMT in alphabetical order when the runtime omits them", () => {
    const zones = getTimeZones(() => [
      "Pacific/Wallis",
      "America/New_York",
      "Europe/Zurich",
    ]);
    expect(zones).toEqual([
      "America/New_York",
      "Europe/Zurich",
      "GMT",
      "Pacific/Wallis",
      "UTC",
    ]);
  });

  it("lists UTC and GMT once when the runtime already has them", () => {
    const zones = getTimeZones(() => ["UTC", "GMT", "Europe/London"]);
    expect(zones).toEqual(["Europe/London", "GMT", "UTC"]);
  });

  it("falls back to a short list when the runtime lookup throws", () => {
    const zones = getTimeZones(() => {
      throw new RangeError("unsupported");
    });
    expect(zones).toContain("America/New_York");
    expect(zones).toContain("UTC");
    expect(zones).toContain("GMT");
    expect(zones).toEqual(sorted(zones));
  });

  it("uses Intl by default, with UTC and GMT each listed once", () => {
    const zones = getTimeZones();
    expect(zones.length).toBeGreaterThan(EXPECTED_MIN_INTL_ZONES);
    expect(zones.filter((z) => z === "UTC")).toHaveLength(1);
    expect(zones.filter((z) => z === "GMT")).toHaveLength(1);
    expect(zones).toEqual(sorted(zones));
  });
});

describe("withTimeZone", () => {
  const zones = ["America/Chicago", "Europe/London", "UTC"];

  it("returns the list unchanged when there is no value", () => {
    expect(withTimeZone(zones, undefined)).toBe(zones);
    expect(withTimeZone(zones, "")).toBe(zones);
  });

  it("returns the list unchanged when the value is already listed", () => {
    expect(withTimeZone(zones, "Europe/London")).toBe(zones);
  });

  it("sorts a value missing from the list into place", () => {
    expect(withTimeZone(zones, "CST")).toEqual([
      "America/Chicago",
      "CST",
      "Europe/London",
      "UTC",
    ]);
    expect(withTimeZone(zones, "US/Eastern")).toEqual([
      "America/Chicago",
      "Europe/London",
      "US/Eastern",
      "UTC",
    ]);
  });
});
