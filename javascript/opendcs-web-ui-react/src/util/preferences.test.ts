import { describe, expect, test } from "vitest";
import type { User } from "opendcs-api";
import {
  planChanges,
  planSessionStart,
  savedOrganization,
  type SyncedSetting,
} from "./preferences";

const COLOR: SyncedSetting = {
  key: "colorMode",
  isValid: (v) => v === "light" || v === "dark" || v === "auto",
  seeds: (v) => v !== "auto",
};
const LANGUAGE: SyncedSetting = {
  key: "language",
  isValid: (v) => v === "en-US" || v === "de-DE",
  seeds: () => false,
};
const SETTINGS = [COLOR, LANGUAGE];

describe("planSessionStart", () => {
  test("applies the profile's values when nothing changed before sign in", () => {
    const local = { colorMode: "light", language: "en-US" };
    const plan = planSessionStart(SETTINGS, local, local, {
      colorMode: "dark",
      language: "de-DE",
    });
    expect(plan).toEqual({ apply: { colorMode: "dark", language: "de-DE" }, save: {} });
  });

  test("leaves settings alone that already match the profile", () => {
    const local = { colorMode: "dark", language: "en-US" };
    const plan = planSessionStart(SETTINGS, local, local, { colorMode: "dark" });
    expect(plan).toEqual({ apply: {}, save: {} });
  });

  test("a choice made before sign in wins over the profile and is saved", () => {
    const plan = planSessionStart(
      SETTINGS,
      { colorMode: "dark", language: "en-US" },
      { colorMode: "light", language: "en-US" },
      { colorMode: "light", language: "de-DE" },
    );
    expect(plan).toEqual({ apply: { language: "de-DE" }, save: { colorMode: "dark" } });
  });

  test("seeds an empty profile with non-default local values only", () => {
    const plan = planSessionStart(
      SETTINGS,
      { colorMode: "dark", language: "de-DE" },
      { colorMode: "dark", language: "de-DE" },
      {},
    );
    // language never seeds, a browser-detected default looks the same as a choice
    expect(plan).toEqual({ apply: {}, save: { colorMode: "dark" } });

    const defaults = { colorMode: "auto", language: "en-US" };
    expect(planSessionStart(SETTINGS, defaults, defaults, {})).toEqual({
      apply: {},
      save: {},
    });
  });

  test("ignores invalid profile values instead of applying them", () => {
    const local = { colorMode: "dark", language: "en-US" };
    const plan = planSessionStart(SETTINGS, local, local, {
      colorMode: "purple",
      language: 7,
    });
    // the invalid colorMode is overwritten by seeding the local value
    expect(plan).toEqual({ apply: {}, save: { colorMode: "dark" } });
  });
});

describe("planChanges", () => {
  test("saves values that changed locally", () => {
    const save = planChanges(
      SETTINGS,
      { colorMode: "dark", language: "en-US" },
      { colorMode: "light", language: "en-US" },
      { colorMode: "light", language: "en-US" },
    );
    expect(save).toEqual({ colorMode: "dark" });
  });

  test("doesn't echo a value that was just applied from the profile", () => {
    const save = planChanges(
      SETTINGS,
      { colorMode: "dark", language: "de-DE" },
      { colorMode: "light", language: "en-US" },
      { colorMode: "dark", language: "de-DE" },
    );
    expect(save).toEqual({});
  });

  test("doesn't save values that didn't change, even if the profile differs", () => {
    // e.g. the profile was updated but a slow apply hasn't landed yet
    const local = { colorMode: "light", language: "en-US" };
    expect(planChanges(SETTINGS, local, local, { language: "de-DE" })).toEqual({});
  });

  test("doesn't save invalid values", () => {
    const save = planChanges(
      SETTINGS,
      { colorMode: "dark", language: "en" },
      { colorMode: "dark", language: "en-US" },
      {},
    );
    expect(save).toEqual({});
  });
});

describe("savedOrganization", () => {
  test("returns the organization name stored in the profile", () => {
    expect(savedOrganization({ preferences: { organization: "SPK" } } as User)).toBe(
      "SPK",
    );
  });

  test("returns undefined for missing, blank or non-string values", () => {
    expect(savedOrganization(undefined)).toBeUndefined();
    expect(savedOrganization({} as User)).toBeUndefined();
    expect(
      savedOrganization({ preferences: { organization: "" } } as User),
    ).toBeUndefined();
    expect(
      savedOrganization({ preferences: { organization: { name: "SPK" } } } as User),
    ).toBeUndefined();
  });
});
