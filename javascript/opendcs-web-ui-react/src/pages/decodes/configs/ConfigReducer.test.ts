import { expect, test } from "vitest";
import { ConfigReducer, type UiConfig } from "./ConfigReducer";

test("save merges payload into the existing config", () => {
  const config: UiConfig = { name: "cfg", description: "old" };

  const result = ConfigReducer(config, {
    type: "save",
    payload: { description: "new" },
  });

  expect(result.name).toEqual("cfg");
  expect(result.description).toEqual("new");
});

test("save_sensor appends when no sensors exist", () => {
  const config: UiConfig = {};

  const result = ConfigReducer(config, {
    type: "save_sensor",
    payload: { sensor: { sensorNumber: 1, sensorName: "Stage" } },
  });

  expect(result.configSensors).toHaveLength(1);
  expect(result.configSensors?.[0].sensorName).toEqual("Stage");
});

test("save_sensor replaces an existing sensor matched by sensorNumber", () => {
  const config: UiConfig = {
    configSensors: [
      { sensorNumber: 1, sensorName: "old" },
      { sensorNumber: 2, sensorName: "keep" },
    ],
  };

  const result = ConfigReducer(config, {
    type: "save_sensor",
    payload: { sensor: { sensorNumber: 1, sensorName: "new" } },
  });

  expect(result.configSensors).toHaveLength(2);
  expect(result.configSensors?.find((s) => s.sensorNumber === 1)?.sensorName).toEqual(
    "new",
  );
  expect(result.configSensors?.find((s) => s.sensorNumber === 2)?.sensorName).toEqual(
    "keep",
  );
});

test("save_sensor uses originalSensorNumber to match when the number itself changes", () => {
  const config: UiConfig = {
    configSensors: [{ sensorNumber: 1, sensorName: "Stage" }],
  };

  const result = ConfigReducer(config, {
    type: "save_sensor",
    payload: {
      sensor: { sensorNumber: 5, sensorName: "Stage renumbered" },
      originalSensorNumber: 1,
    },
  });

  expect(result.configSensors).toHaveLength(1);
  expect(result.configSensors?.[0]).toEqual({
    sensorNumber: 5,
    sensorName: "Stage renumbered",
  });
});

test("delete_sensor removes the sensor with the matching number", () => {
  const config: UiConfig = {
    configSensors: [{ sensorNumber: 1 }, { sensorNumber: 2 }],
  };

  const result = ConfigReducer(config, {
    type: "delete_sensor",
    payload: { sensorNumber: 1 },
  });

  expect(result.configSensors).toHaveLength(1);
  expect(result.configSensors?.[0].sensorNumber).toEqual(2);
});

test("save_script appends when no scripts exist", () => {
  const config: UiConfig = {};

  const result = ConfigReducer(config, {
    type: "save_script",
    payload: { script: { name: "script1", headerType: "goes" } },
  });

  expect(result.scripts).toHaveLength(1);
  expect(result.scripts?.[0].name).toEqual("script1");
});

test("save_script replaces an existing script matched by name", () => {
  const config: UiConfig = {
    scripts: [
      { name: "s1", headerType: "goes" },
      { name: "s2", headerType: "other" },
    ],
  };

  const result = ConfigReducer(config, {
    type: "save_script",
    payload: { script: { name: "s1", headerType: "iridium" } },
  });

  expect(result.scripts).toHaveLength(2);
  expect(result.scripts?.find((s) => s.name === "s1")?.headerType).toEqual("iridium");
  expect(result.scripts?.find((s) => s.name === "s2")?.headerType).toEqual("other");
});

test("save_script uses originalName to match when the name itself changes", () => {
  const config: UiConfig = {
    scripts: [{ name: "old", headerType: "goes" }],
  };

  const result = ConfigReducer(config, {
    type: "save_script",
    payload: {
      script: { name: "renamed", headerType: "goes" },
      originalName: "old",
    },
  });

  expect(result.scripts).toHaveLength(1);
  expect(result.scripts?.[0]).toEqual({ name: "renamed", headerType: "goes" });
});

test("delete_script removes the script with the matching name", () => {
  const config: UiConfig = {
    scripts: [{ name: "s1" }, { name: "s2" }],
  };

  const result = ConfigReducer(config, {
    type: "delete_script",
    payload: { name: "s1" },
  });

  expect(result.scripts).toHaveLength(1);
  expect(result.scripts?.[0].name).toEqual("s2");
});
