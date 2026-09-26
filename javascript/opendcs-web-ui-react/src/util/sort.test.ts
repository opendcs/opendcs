import { describe, expect, test } from "vitest";
import { compareStrings } from "./sort";

const sorted = (values: (string | undefined)[]) => [...values].sort(compareStrings);

describe("compareStrings", () => {
  test("interleaves upper and lower case instead of grouping by case", () => {
    expect(sorted(["SWT", "acre-ft", "MVR", "cfs", "NWO"])).toEqual([
      "acre-ft",
      "cfs",
      "MVR",
      "NWO",
      "SWT",
    ]);
  });

  test("orders embedded numbers numerically", () => {
    expect(sorted(["EU10", "EU9", "EU1"])).toEqual(["EU1", "EU9", "EU10"]);
  });

  test("treats a missing value as empty rather than throwing", () => {
    // Called directly: Array.prototype.sort never hands undefined to a
    // comparator, so this contract only shows up at the call site.
    expect(compareStrings(undefined, "a")).toBeLessThan(0);
    expect(compareStrings("a", null)).toBeGreaterThan(0);
    expect(compareStrings(undefined, null)).toBe(0);
  });
});
