import { describe, expect, it } from "vitest";
import {
  apiTimeToEditorValue,
  editorValueToStartFields,
  editorValueToEndFields,
} from "./computationTime";

describe("apiTimeToEditorValue", () => {
  it.each([
    { type: "Now", date: undefined, interval: undefined, expected: "now" },
    { type: "Now -", date: undefined, interval: "1 hour", expected: "now - 1 hour" },
    { type: "Now -", date: undefined, interval: undefined, expected: undefined },
    { type: "No Limit", date: undefined, interval: undefined, expected: undefined },
    { type: undefined, date: undefined, interval: undefined, expected: undefined },
  ])(
    "type=$type interval=$interval → $expected",
    ({ type, date, interval, expected }) => {
      expect(apiTimeToEditorValue(type, date, interval)).toBe(expected);
    },
  );

  it("Calendar with a valid Date formats as YYYY/DDD HH:MM:SS", () => {
    expect(
      apiTimeToEditorValue(
        "Calendar",
        new Date(Date.UTC(2026, 4, 28, 18, 8, 19)),
        undefined,
      ),
    ).toBe("2026/148 18:08:19");
  });

  it("Calendar with a date string coerces and formats", () => {
    expect(
      apiTimeToEditorValue(
        "Calendar",
        "2026-05-28T18:08:19.000Z" as unknown as Date,
        undefined,
      ),
    ).toBe("2026/148 18:08:19");
  });

  it("Calendar with null date → undefined", () => {
    expect(apiTimeToEditorValue("Calendar", null, undefined)).toBeUndefined();
  });

  it("Calendar with an invalid Date → undefined", () => {
    expect(
      apiTimeToEditorValue("Calendar", new Date("bad"), undefined),
    ).toBeUndefined();
  });
});

// Both editorValueToStart/EndFields share identical branching — test them together.
describe("editorValueToStartFields / editorValueToEndFields", () => {
  it.each([
    {
      label: "empty string → No Limit",
      input: "",
      startType: "No Limit",
      startInterval: undefined,
      endType: "No Limit",
      endInterval: undefined,
    },
    {
      label: "now → Now",
      input: "now",
      startType: "Now",
      startInterval: undefined,
      endType: "Now",
      endInterval: undefined,
    },
    {
      label: "now-minus → Now - with interval",
      input: "now - 30 minutes",
      startType: "Now -",
      startInterval: "30 minutes",
      endType: "Now -",
      endInterval: "30 minutes",
    },
    {
      label: "unrecognised string → No Limit fallback",
      input: "garbage",
      startType: "No Limit",
      startInterval: undefined,
      endType: "No Limit",
      endInterval: undefined,
    },
  ])("$label", ({ input, startType, startInterval, endType, endInterval }) => {
    expect(editorValueToStartFields(input)).toEqual({
      effectiveStartType: startType,
      effectiveStartInterval: startInterval,
      effectiveStartDate: undefined,
    });
    expect(editorValueToEndFields(input)).toEqual({
      effectiveEndType: endType,
      effectiveEndInterval: endInterval,
      effectiveEndDate: undefined,
    });
  });

  it("Calendar input produces a Date instance in both start and end", () => {
    const start = editorValueToStartFields("2026/148 18:08:19");
    expect(start.effectiveStartType).toBe("Calendar");
    expect(start.effectiveStartDate).toBeInstanceOf(Date);
    expect(start.effectiveStartInterval).toBeUndefined();

    const end = editorValueToEndFields("2026/148 18:08:19");
    expect(end.effectiveEndType).toBe("Calendar");
    expect(end.effectiveEndDate).toBeInstanceOf(Date);
    expect(end.effectiveEndInterval).toBeUndefined();
  });
});

describe("round-trip: apiTimeToEditorValue → editorValueToStartFields", () => {
  it("Now - round-trips the interval", () => {
    const v = apiTimeToEditorValue("Now -", undefined, "30 minutes")!;
    const r = editorValueToStartFields(v);
    expect(r.effectiveStartType).toBe("Now -");
    expect(r.effectiveStartInterval).toBe("30 minutes");
  });

  it("Calendar round-trips the date", () => {
    const date = new Date(Date.UTC(2025, 6, 15, 9, 30, 45));
    const v = apiTimeToEditorValue("Calendar", date, undefined)!;
    const r = editorValueToStartFields(v);
    expect(r.effectiveStartType).toBe("Calendar");
    expect(r.effectiveStartDate?.getTime()).toBe(date.getTime());
  });
});
