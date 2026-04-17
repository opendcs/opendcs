import { expect, test } from "vitest";
import type { ApiAppRef, ApiComputation, ApiTsGroupRef } from "opendcs-api";
import { normalizeComputationForSave } from "./computationSave";

type UiComputation = Partial<ApiComputation>;

const processOptions: ApiAppRef[] = [
  { appId: 200, appName: "compproc", appType: "ComputationProcess" },
  { appId: 201, appName: "routingproc", appType: "RoutingProcess" },
];

const groupOptions: ApiTsGroupRef[] = [
  { groupId: 5, groupName: "Daily", groupType: "Computation" },
  { groupId: 6, groupName: "Hourly", groupType: "Computation" },
];

test("normalizeComputationForSave resolves valid ids from option names", () => {
  const computation: UiComputation = {
    computationId: 1,
    applicationName: "routingproc",
    groupName: "Hourly",
  };

  const result = normalizeComputationForSave(computation, processOptions, groupOptions);

  expect(result.appId).toEqual(201);
  expect(result.applicationName).toEqual("routingproc");
  expect(result.groupId).toEqual(6);
  expect(result.groupName).toEqual("Hourly");
});

test("normalizeComputationForSave ignores OpenDCS null-key ids", () => {
  const computation: UiComputation = {
    computationId: 1,
    appId: -1,
    applicationName: "compproc",
    groupId: -1,
    groupName: "Daily",
  };

  const result = normalizeComputationForSave(computation, processOptions, groupOptions);

  expect(result.appId).toEqual(200);
  expect(result.applicationName).toEqual("compproc");
  expect(result.groupId).toEqual(5);
  expect(result.groupName).toEqual("Daily");
});

test("normalizeComputationForSave clears invalid ids when nothing matches", () => {
  const computation: UiComputation = {
    computationId: 1,
    appId: -1,
    applicationName: "",
    groupId: -1,
    groupName: "",
  };

  const result = normalizeComputationForSave(computation, processOptions, groupOptions);

  expect(result.appId).toBeUndefined();
  expect(result.applicationName).toEqual("");
  expect(result.groupId).toBeUndefined();
  expect(result.groupName).toEqual("");
});
