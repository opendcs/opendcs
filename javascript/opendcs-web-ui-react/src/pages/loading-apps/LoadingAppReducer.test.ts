import { expect, test } from "vitest";
import type { UiLoadingApp } from "./LoadingApp";
import { LoadingAppReducer } from "./LoadingAppReducer";

test("Save merges scalar fields into current app", () => {
  const start: UiLoadingApp = {};

  const named = LoadingAppReducer(start, {
    type: "save",
    payload: { appName: "compproc" },
  });
  expect(named.appName).toEqual("compproc");

  const typed = LoadingAppReducer(named, {
    type: "save",
    payload: { appType: "computationprocess", comment: "Main loop" },
  });
  expect(typed.appName).toEqual("compproc");
  expect(typed.appType).toEqual("computationprocess");
  expect(typed.comment).toEqual("Main loop");
});

test("Save toggles manualEditingApp", () => {
  const start: UiLoadingApp = { manualEditingApp: false };
  const toggled = LoadingAppReducer(start, {
    type: "save",
    payload: { manualEditingApp: true },
  });
  expect(toggled.manualEditingApp).toEqual(true);
});

test("Add, update, and delete properties", () => {
  const start: UiLoadingApp = {};

  const added = LoadingAppReducer(start, {
    type: "save_prop",
    payload: { name: "OfficeID", value: "CWMS" },
  });
  expect(added.properties?.OfficeID).toEqual("CWMS");

  const updated = LoadingAppReducer(added, {
    type: "save_prop",
    payload: { name: "OfficeID", value: "SWT" },
  });
  expect(updated.properties?.OfficeID).toEqual("SWT");

  const withSecond = LoadingAppReducer(updated, {
    type: "save_prop",
    payload: { name: "logFile", value: "compproc.log" },
  });
  expect(withSecond.properties?.OfficeID).toEqual("SWT");
  expect(withSecond.properties?.logFile).toEqual("compproc.log");

  const deleted = LoadingAppReducer(withSecond, {
    type: "delete_prop",
    payload: { name: "OfficeID" },
  });
  expect(deleted.properties?.OfficeID).toBeUndefined();
  expect(deleted.properties?.logFile).toEqual("compproc.log");
});

test("Delete on missing property leaves other state intact", () => {
  const start: UiLoadingApp = {
    appName: "dbimport",
    properties: { logFile: "dbimport.log" },
  };

  const deleted = LoadingAppReducer(start, {
    type: "delete_prop",
    payload: { name: "missing" },
  });
  expect(deleted.appName).toEqual("dbimport");
  expect(deleted.properties?.logFile).toEqual("dbimport.log");
});

test("Save does not clobber unrelated nested collections", () => {
  const start: UiLoadingApp = {
    appName: "compproc",
    properties: { OfficeID: "CWMS" },
  };

  const result = LoadingAppReducer(start, {
    type: "save",
    payload: { comment: "now with a comment" },
  });
  expect(result.appName).toEqual("compproc");
  expect(result.comment).toEqual("now with a comment");
  expect(result.properties?.OfficeID).toEqual("CWMS");
});
