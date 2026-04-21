import { expect, test, vi } from "vitest";
import type {
  ApiAppRef,
  ApiCompParm,
  ApiComputation,
  ApiTsGroupRef,
} from "opendcs-api";
import { normalizeComputationForSave, saveComputationDraft } from "./computationSave";
import { mergeAlgorithmParms, mergeAlgorithmProps } from "./computationWorkspace";

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

  const result = normalizeComputationForSave(computation, [], groupOptions);

  expect(result.appId).toBeUndefined();
  expect(result.applicationName).toEqual("");
  expect(result.groupId).toBeUndefined();
  expect(result.groupName).toEqual("");
});

test("normalizeComputationForSave defaults to a computation process when none selected", () => {
  const computation: UiComputation = {
    computationId: -1,
    name: "Draft",
  };

  const result = normalizeComputationForSave(
    computation,
    [
      { appId: 300, appName: "routing", appType: "RoutingProcess" },
      { appId: 301, appName: "compproc", appType: "ComputationProcess" },
    ],
    groupOptions,
  );

  expect(result.appId).toEqual(301);
  expect(result.applicationName).toEqual("compproc");
});

test("saveComputationDraft returns the async save result", async () => {
  const computation: UiComputation = {
    computationId: 7,
    name: "Updated Name",
    algorithmId: 10,
    algorithmName: "AverageAlgorithm",
    applicationName: "compproc",
    groupName: "Daily",
    props: { foo: "bar" },
  };
  const parmList: ApiCompParm[] = [{ algoRoleName: "input", algoParmType: "i" }];
  const saved: ApiComputation = {
    computationId: 70,
    name: "Saved Name",
    appId: 200,
    applicationName: "compproc",
    groupId: 5,
    groupName: "Daily",
    props: { foo: "bar" },
    parmList,
  };
  const save = vi.fn(async () => saved);

  const result = await saveComputationDraft(
    computation,
    parmList,
    processOptions,
    groupOptions,
    save,
  );

  expect(save).toHaveBeenCalledWith(
    expect.objectContaining({
      computationId: 7,
      name: "Updated Name",
      algorithmId: 10,
      algorithmName: "AverageAlgorithm",
      appId: 200,
      applicationName: "compproc",
      groupId: 5,
      groupName: "Daily",
      props: { foo: "bar" },
      parmList,
    }),
  );
  expect(result).toEqual(saved);
});

test("saveComputationDraft fails locally when required fields are missing", async () => {
  await expect(
    saveComputationDraft({}, [], processOptions, groupOptions, vi.fn()),
  ).rejects.toThrow("Enter a computation name before saving.");

  await expect(
    saveComputationDraft({ name: "Draft" }, [], processOptions, groupOptions, vi.fn()),
  ).rejects.toThrow("Select an algorithm before saving.");
});

test("saveComputationDraft allows saving without a process when none are available", async () => {
  const save = vi.fn();

  await saveComputationDraft(
    {
      name: "Draft",
      algorithmId: 10,
      algorithmName: "AverageAlgorithm",
    },
    [],
    [],
    groupOptions,
    save,
  );

  expect(save).toHaveBeenCalledWith(
    expect.objectContaining({
      name: "Draft",
      algorithmId: 10,
      appId: undefined,
      applicationName: "",
    }),
  );
});

test("saveComputationDraft strips temporary ids and incomplete parameter keys", async () => {
  const computation: UiComputation = {
    computationId: -1,
    algorithmId: 10,
    algorithmName: "AverageAlgorithm",
    appId: -1,
    groupId: -1,
    lastModified: new Date("2026-04-21T12:00:00Z"),
    name: "Draft",
    props: {
      "": "ignored",
      output: "stored",
    },
  };
  const parmList: ApiCompParm[] = [
    {
      algoRoleName: "input",
      algoParmType: "i",
      dataTypeId: -1,
      dataType: "Stage",
      siteId: -1,
      tsKey: -1,
    },
    {
      algoRoleName: "output",
      algoParmType: "o",
      dataTypeId: 12,
      dataType: "SHEF-PE:Flow",
      siteId: 44,
    },
  ];
  const save = vi.fn();

  await saveComputationDraft(computation, parmList, processOptions, groupOptions, save);

  expect(save).toHaveBeenCalledWith(
    expect.objectContaining({
      computationId: undefined,
      algorithmId: 10,
      appId: 200,
      applicationName: "compproc",
      groupId: undefined,
      lastModified: undefined,
      props: { output: "stored" },
      parmList: [
        expect.objectContaining({
          algoRoleName: "input",
          dataTypeId: undefined,
          dataType: undefined,
          siteId: undefined,
          tsKey: undefined,
        }),
        expect.objectContaining({
          algoRoleName: "output",
          dataTypeId: 12,
          dataType: "SHEF-PE:Flow",
          siteId: 44,
        }),
      ],
    }),
  );
});

test("mergeAlgorithmParms populates required parameter roles", () => {
  const result = mergeAlgorithmParms(
    [
      {
        algoRoleName: "input",
        algoParmType: "i",
        siteName: "TESTSITE",
      },
      {
        algoRoleName: "legacy",
        algoParmType: "i",
        siteName: "LEGACY",
      },
    ],
    [
      { roleName: "input", parmType: "i" },
      { roleName: "output", parmType: "o" },
    ],
  );

  expect(result).toEqual([
    expect.objectContaining({
      algoRoleName: "input",
      algoParmType: "i",
      siteName: "TESTSITE",
    }),
    expect.objectContaining({
      algoRoleName: "output",
      algoParmType: "o",
    }),
    expect.objectContaining({
      algoRoleName: "legacy",
      algoParmType: "i",
      siteName: "LEGACY",
    }),
  ]);
});

test("mergeAlgorithmProps populates specs and algorithm defaults without overwriting existing values", () => {
  const result = mergeAlgorithmProps(
    {
      interval: "1Hour",
      custom: "existing",
    },
    {
      interval: "1Day",
      units: "cfs",
    },
    [
      { name: "interval", type: "s", description: "Interval" },
      { name: "timezone", type: "t", description: "Time zone" },
    ],
  );

  expect(result).toEqual({
    interval: "1Hour",
    timezone: "",
    units: "cfs",
    custom: "existing",
  });
});
