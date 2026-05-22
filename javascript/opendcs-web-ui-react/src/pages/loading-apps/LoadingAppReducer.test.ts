import { expect, test } from "vitest";
import type { UiLoadingApp } from "./LoadingApp";
import { LoadingAppReducer } from "./LoadingAppReducer";

test("save_prop adds a new property when none exist", () => {
  const app: UiLoadingApp = {};

  const result = LoadingAppReducer(app, {
    type: "save_prop",
    payload: { name: "pollingInterval", value: "60" },
  });

  expect(result.properties).toBeDefined();
  expect(result.properties?.pollingInterval).toEqual("60");
});

test("save_prop overwrites an existing property and preserves others", () => {
  const app: UiLoadingApp = {
    properties: { pollingInterval: "30", logLevel: "DEBUG" },
  };

  const result = LoadingAppReducer(app, {
    type: "save_prop",
    payload: { name: "pollingInterval", value: "60" },
  });

  expect(result.properties?.pollingInterval).toEqual("60");
  expect(result.properties?.logLevel).toEqual("DEBUG");
});

test("delete_prop removes the named property and keeps the rest", () => {
  const app: UiLoadingApp = {
    properties: { pollingInterval: "60", logLevel: "DEBUG" },
  };

  const result = LoadingAppReducer(app, {
    type: "delete_prop",
    payload: { name: "pollingInterval" },
  });

  expect(result.properties?.pollingInterval).toBeUndefined();
  expect(result.properties?.logLevel).toEqual("DEBUG");
});

test("delete_prop on a missing key leaves properties unchanged", () => {
  const app: UiLoadingApp = { properties: { logLevel: "DEBUG" } };

  const result = LoadingAppReducer(app, {
    type: "delete_prop",
    payload: { name: "nonexistent" },
  });

  expect(result.properties).toEqual({ logLevel: "DEBUG" });
});

test("save merges payload fields into the existing app", () => {
  const app: UiLoadingApp = { appName: "compproc", appType: "computationprocess" };

  const result = LoadingAppReducer(app, {
    type: "save",
    payload: { appType: "utility", comment: "updated" },
  });

  expect(result.appName).toEqual("compproc");
  expect(result.appType).toEqual("utility");
  expect(result.comment).toEqual("updated");
});

test("save with manualEditingApp toggles the boolean field", () => {
  const app: UiLoadingApp = { manualEditingApp: false };

  const result = LoadingAppReducer(app, {
    type: "save",
    payload: { manualEditingApp: true },
  });

  expect(result.manualEditingApp).toBe(true);
});
