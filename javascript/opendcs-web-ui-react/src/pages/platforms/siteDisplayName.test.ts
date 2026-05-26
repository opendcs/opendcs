import { describe, it, expect } from "vitest";
import { siteDisplayName } from "./siteDisplayName";

describe("siteDisplayName", () => {
  it("returns publicName when present", () => {
    expect(siteDisplayName({ siteId: 1, publicName: "RiverPoint" })).toBe("RiverPoint");
  });

  it("falls back to first sitename value when publicName is missing", () => {
    expect(siteDisplayName({ siteId: 2, sitenames: { "USGS-ID": "01646500" } })).toBe(
      "01646500",
    );
  });

  it("ignores empty publicName and uses sitename instead", () => {
    expect(
      siteDisplayName({
        siteId: 3,
        publicName: "",
        sitenames: { local: "TackleBox" },
      }),
    ).toBe("TackleBox");
  });

  it("falls back to siteId string when no names are available", () => {
    expect(siteDisplayName({ siteId: 42 })).toBe("42");
  });

  it("returns empty string when nothing identifies the site", () => {
    expect(siteDisplayName({})).toBe("");
  });
});
