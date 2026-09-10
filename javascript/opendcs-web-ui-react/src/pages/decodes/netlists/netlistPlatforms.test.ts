import { describe, expect, test } from "vitest";
import type { ApiPlatformRef } from "opendcs-api";
import {
  platformCandidates,
  platformNameFor,
  toNetlistItem,
  transportIdFor,
} from "./netlistPlatforms";

const GOES_PLATFORM: ApiPlatformRef = {
  platformId: 1,
  name: "BFD-goes",
  agency: "CWMS",
  config: "BFD-CFG",
  description: "Buford Dam",
  transportMedia: { "goes-self": "BFDBMD01", polltype: "none" },
  sitenames: { NWSHB5: "BFD", CWMS: "Buford" },
};

const IRIDIUM_PLATFORM: ApiPlatformRef = {
  platformId: 2,
  name: "Ice-Station",
  transportMedia: { iridium: "300234010000000" },
};

describe("transportIdFor", () => {
  test("matches a GOES netlist against any GOES medium", () => {
    expect(transportIdFor(GOES_PLATFORM, "goes")).toBe("BFDBMD01");
    expect(transportIdFor(GOES_PLATFORM, "goes-random")).toBe("BFDBMD01");
  });

  test("prefers an exact medium type over another GOES type", () => {
    const both: ApiPlatformRef = {
      transportMedia: { "goes-random": "RANDOM01", "goes-self": "SELF0001" },
    };
    expect(transportIdFor(both, "goes-self")).toBe("SELF0001");
  });

  test("matches non-GOES types exactly and case-insensitively", () => {
    expect(transportIdFor(IRIDIUM_PLATFORM, "Iridium")).toBe("300234010000000");
    expect(transportIdFor(IRIDIUM_PLATFORM, "goes")).toBeUndefined();
  });

  test("returns nothing when the netlist has no medium type", () => {
    expect(transportIdFor(GOES_PLATFORM, undefined)).toBeUndefined();
    expect(transportIdFor(GOES_PLATFORM, " ")).toBeUndefined();
  });
});

describe("platformNameFor", () => {
  test("uses the site name of the preferred type", () => {
    expect(platformNameFor(GOES_PLATFORM, "nwshb5")).toBe("BFD");
    expect(platformNameFor(GOES_PLATFORM, "cwms")).toBe("Buford");
  });

  test("falls back to the platform name", () => {
    expect(platformNameFor(GOES_PLATFORM, "usgs")).toBe("BFD-goes");
    expect(platformNameFor(GOES_PLATFORM, undefined)).toBe("BFD-goes");
  });
});

describe("platformCandidates", () => {
  test("keeps only platforms with a medium of the netlist's type", () => {
    const candidates = platformCandidates(
      [GOES_PLATFORM, IRIDIUM_PLATFORM],
      "goes",
      "nwshb5",
    );
    expect(candidates).toEqual([
      {
        platformId: 1,
        platform: "BFD-goes",
        transportId: "BFDBMD01",
        platformName: "BFD",
        agency: "CWMS",
        config: "BFD-CFG",
        description: "Buford Dam",
      },
    ]);
  });
});

describe("toNetlistItem", () => {
  test("builds the netlist item from a candidate", () => {
    const [candidate] = platformCandidates([GOES_PLATFORM], "goes", "nwshb5");
    expect(toNetlistItem(candidate)).toEqual({
      transportId: "BFDBMD01",
      platformName: "BFD",
      description: "Buford Dam",
    });
  });
});
