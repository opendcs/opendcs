import { expect, test } from "vitest";
import type { UiPlatform } from "./Platform";
import { PlatformReducer } from "./PlatformReducer";

test("save_prop adds a new property when none exist", () => {
  const platform: UiPlatform = {};

  const result = PlatformReducer(platform, {
    type: "save_prop",
    payload: { name: "prop1", value: "value1" },
  });

  expect(result.properties).toBeDefined();
  expect(result.properties?.prop1).toEqual("value1");
});

test("save_prop overwrites an existing property and preserves others", () => {
  const platform: UiPlatform = {
    properties: { prop1: "old", prop2: "keep" },
  };

  const result = PlatformReducer(platform, {
    type: "save_prop",
    payload: { name: "prop1", value: "new" },
  });

  expect(result.properties?.prop1).toEqual("new");
  expect(result.properties?.prop2).toEqual("keep");
});

test("delete_prop removes the property and keeps the rest", () => {
  const platform: UiPlatform = {
    properties: { prop1: "value1", prop2: "value2" },
  };

  const result = PlatformReducer(platform, {
    type: "delete_prop",
    payload: { name: "prop1" },
  });

  expect(result.properties?.prop1).toBeUndefined();
  expect(result.properties?.prop2).toEqual("value2");
});

test("delete_prop is a no-op when properties is undefined", () => {
  const platform: UiPlatform = {};

  const result = PlatformReducer(platform, {
    type: "delete_prop",
    payload: { name: "missing" },
  });

  expect(result.properties).toBeDefined();
  expect(Object.keys(result.properties!)).toHaveLength(0);
});

test("save merges payload into the existing platform", () => {
  const platform: UiPlatform = { name: "p1", description: "old" };

  const result = PlatformReducer(platform, {
    type: "save",
    payload: { description: "new", agency: "USGS" },
  });

  expect(result.name).toEqual("p1");
  expect(result.description).toEqual("new");
  expect(result.agency).toEqual("USGS");
});
