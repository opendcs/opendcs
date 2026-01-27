import { expect, test } from "vitest";
import type { UiSite } from "./Site";
import { SiteReducer } from "./SiteReducer";

test("Reducer Add SiteName", () => {
  const testSite: UiSite = {};

  const result = SiteReducer(testSite, {
    type: "add_name",
    payload: { type: "CWMS", name: "Test Site 1" },
  });
  console.log(result);
  expect(result).toBeDefined();
  expect(result.sitenames).toBeDefined();

  expect(result.sitenames?.CWMS).toEqual("Test Site 1");
});

test("Change a SiteName Type", () => {
  const testSite: UiSite = {
    sitenames: {
      CWMS: "Test 1",
      NWSHDB5: "Test1",
    },
  };

  expect(testSite.sitenames).toBeDefined();
  expect(testSite.sitenames?.CWMS).toEqual("Test 1");
  const result = SiteReducer(testSite, {
    type: "change_type",
    payload: { old_type: "CWMS", new_type: "local" },
  });
  expect(result).toBeDefined();
  expect(result.sitenames?.CWMS).not.toBeDefined();
  expect(result.sitenames?.local).toEqual("Test 1");
});

test("Modify properties", () => {
  const testSite: UiSite = {
    sitenames: {
      CWMS: "Test Mod Properties",
      NWSHDB5: "Test1",
    },
  };

  const resultAddProp = SiteReducer(testSite, {
    type: "save_prop",
    payload: { name: "prop1", value: "value1" },
  });
  expect(resultAddProp).toBeDefined();
  expect(resultAddProp.properties?.prop1).toEqual("value1");

  const resultEditProp = SiteReducer(resultAddProp, {
    type: "save_prop",
    payload: { name: "prop1", value: "value2" },
  });
  console.log(resultEditProp);
  expect(resultEditProp.properties?.prop1).toEqual("value2");

  const resultDeleteProp = SiteReducer(resultEditProp, {
    type: "delete_prop",
    payload: { name: "prop1" },
  });
  expect(resultDeleteProp.properties?.prop1).toBeUndefined();
});
