import { describe, it, expect } from "vitest";
import { preferredPlatformName } from "./preferredPlatformName";

describe("preferredPlatformName", () => {
  it("returns the fallback when no preference is set", () => {
    expect(
      preferredPlatformName({ CWMS: "ACIA", NWSHB5: "ABRN1" }, undefined, "", "ABRN1"),
    ).toBe("ABRN1");
  });

  it("returns the fallback when the site has no names", () => {
    expect(preferredPlatformName(undefined, undefined, "CWMS", "fallback")).toBe(
      "fallback",
    );
    expect(preferredPlatformName({}, undefined, "CWMS", "fallback")).toBe("fallback");
  });

  it("uses the name matching the preferred type", () => {
    expect(
      preferredPlatformName(
        { CWMS: "ACIA", NWSHB5: "ABRN1" },
        undefined,
        "CWMS",
        "ABRN1",
      ),
    ).toBe("ACIA");
  });

  it("matches the preferred type case-insensitively", () => {
    expect(preferredPlatformName({ cwms: "ACIA" }, undefined, "CWMS", "fallback")).toBe(
      "ACIA",
    );
  });

  it("falls back to the first available name when the preferred type is absent", () => {
    expect(
      preferredPlatformName({ NWSHB5: "ABRN1" }, undefined, "CWMS", "fallback"),
    ).toBe("ABRN1");
  });

  it("appends the designator, mirroring Platform#makeFileName", () => {
    expect(preferredPlatformName({ CWMS: "ACIA" }, "goes", "CWMS", "fallback")).toBe(
      "ACIA-goes",
    );
  });
});
