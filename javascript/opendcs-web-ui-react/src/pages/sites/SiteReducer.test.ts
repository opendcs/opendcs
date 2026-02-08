import { expect, test } from "vitest";
import type { UiSite } from "./Site";
import { SiteReducer } from "./SiteReducer";

test("Reducer Add SiteName", () => {
  const testSite: UiSite = {};

  const result = SiteReducer(testSite, {
    type: "add_name",
    payload: { type: "cwms", name: "Test Site 1" },
  });
  console.log(result);
  expect(result).toBeDefined();
  expect(result.sitenames).toBeDefined();

  expect(result.sitenames?.cwms).toEqual("Test Site 1");
});

test("Change a SiteName Then Delete", () => {
  const testSite: UiSite = {
    sitenames: {
      cwms: "Test 1",
      nwshb5: "Test1",
    },
  };

  expect(testSite.sitenames).toBeDefined();
  expect(testSite.sitenames?.cwms).toEqual("Test 1");
  const result = SiteReducer(testSite, {
    type: "add_name",
    payload: { type: "cwms", name: "Test Rename" },
  });
  expect(result).toBeDefined();
  expect(result.sitenames?.cwms).toEqual("Test Rename");

  const deletedName = SiteReducer(result, {
    type: "delete_name",
    payload: { type: "local" },
  });
  expect(deletedName.sitenames?.local).not.toBeDefined();
});

test("Modify properties", () => {
  const testSite: UiSite = {
    sitenames: {
      cwms: "Test Mod Properties",
      nwshb5: "Test1",
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

test("Modify Site itself", () => {
  const testSite: UiSite = {}; // start new

  const withPublicName = SiteReducer(testSite, {
    type: "save",
    payload: { publicName: "A test site" },
  });
  expect(withPublicName.publicName).toEqual("A test site");

  const withNames = SiteReducer(withPublicName, {
    type: "add_name",
    payload: { type: "cwms", name: "Test Site full" },
  });
  expect(withNames.sitenames).toBeDefined();

  const setLatLongElv = SiteReducer(withNames, {
    type: "save",
    payload: {
      elevation: 20.0,
      elevUnits: "ft",
      latitude: "89.0",
      longitude: "-120.0",
    },
  });
  expect(setLatLongElv.elevUnits).toEqual("ft");
  expect(setLatLongElv.elevation).toEqual(20.0);
  expect(setLatLongElv.latitude).toEqual("89.0");
  expect(setLatLongElv.longitude).toEqual("-120.0");
  expect(setLatLongElv.sitenames?.cwms).toBeDefined(); // make sure we haven't lost something important.
});
