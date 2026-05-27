import { expect, test } from "vitest";
import type { UiPlatform } from "./Platform";
import { PlatformReducer } from "./PlatformReducer";
import { transportKey } from "./transportKey";

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

test("save_sensor appends when no sensors exist", () => {
  const platform: UiPlatform = {};

  const result = PlatformReducer(platform, {
    type: "save_sensor",
    payload: { sensor: { sensorNum: 1, min: 0, max: 10 } },
  });

  expect(result.platformSensors).toHaveLength(1);
  expect(result.platformSensors?.[0]).toEqual({ sensorNum: 1, min: 0, max: 10 });
});

test("save_sensor updates an existing sensor matched by sensorNum", () => {
  const platform: UiPlatform = {
    platformSensors: [
      { sensorNum: 1, min: 0 },
      { sensorNum: 2, min: 5 },
    ],
  };

  const result = PlatformReducer(platform, {
    type: "save_sensor",
    payload: { sensor: { sensorNum: 1, min: 100, max: 200 } },
  });

  expect(result.platformSensors).toHaveLength(2);
  expect(result.platformSensors?.[0]).toEqual({
    sensorNum: 1,
    min: 100,
    max: 200,
  });
  expect(result.platformSensors?.[1]).toEqual({ sensorNum: 2, min: 5 });
});

test("save_sensor uses originalSensorNum when sensorNum was renumbered", () => {
  const platform: UiPlatform = {
    platformSensors: [{ sensorNum: -1, min: 0 }],
  };

  const result = PlatformReducer(platform, {
    type: "save_sensor",
    payload: {
      sensor: { sensorNum: 7, min: 1 },
      originalSensorNum: -1,
    },
  });

  expect(result.platformSensors).toHaveLength(1);
  expect(result.platformSensors?.[0]).toEqual({ sensorNum: 7, min: 1 });
});

test("delete_sensor removes the matching sensor", () => {
  const platform: UiPlatform = {
    platformSensors: [{ sensorNum: 1 }, { sensorNum: 2 }],
  };

  const result = PlatformReducer(platform, {
    type: "delete_sensor",
    payload: { sensorNum: 1 },
  });

  expect(result.platformSensors).toHaveLength(1);
  expect(result.platformSensors?.[0]).toEqual({ sensorNum: 2 });
});

test("save_transport appends when no transport media exist", () => {
  const platform: UiPlatform = {};

  const result = PlatformReducer(platform, {
    type: "save_transport",
    payload: { medium: { mediumType: "GOES", mediumId: "ABCD1234" } },
  });

  expect(result.transportMedia).toHaveLength(1);
  expect(result.transportMedia?.[0]).toEqual({
    mediumType: "GOES",
    mediumId: "ABCD1234",
  });
});

test("save_transport updates an existing medium matched by composite key", () => {
  const platform: UiPlatform = {
    transportMedia: [
      { mediumType: "GOES", mediumId: "ABCD" },
      { mediumType: "IRIDIUM", mediumId: "0000" },
    ],
  };

  const result = PlatformReducer(platform, {
    type: "save_transport",
    payload: {
      medium: { mediumType: "GOES", mediumId: "ABCD", channelNum: 99 },
    },
  });

  expect(result.transportMedia).toHaveLength(2);
  expect(result.transportMedia?.[0]).toEqual({
    mediumType: "GOES",
    mediumId: "ABCD",
    channelNum: 99,
  });
});

test("save_transport uses originalKey when identity fields changed", () => {
  const platform: UiPlatform = {
    transportMedia: [{ mediumType: "GOES", mediumId: "new-1" }],
  };
  const originalKey = transportKey({ mediumType: "GOES", mediumId: "new-1" });

  const result = PlatformReducer(platform, {
    type: "save_transport",
    payload: {
      medium: { mediumType: "GOES", mediumId: "REAL_ID" },
      originalKey,
    },
  });

  expect(result.transportMedia).toHaveLength(1);
  expect(result.transportMedia?.[0]).toEqual({
    mediumType: "GOES",
    mediumId: "REAL_ID",
  });
});

test("delete_transport removes the medium matching the composite key", () => {
  const platform: UiPlatform = {
    transportMedia: [
      { mediumType: "GOES", mediumId: "ABCD" },
      { mediumType: "IRIDIUM", mediumId: "0000" },
    ],
  };

  const result = PlatformReducer(platform, {
    type: "delete_transport",
    payload: {
      key: transportKey({ mediumType: "GOES", mediumId: "ABCD" }),
    },
  });

  expect(result.transportMedia).toHaveLength(1);
  expect(result.transportMedia?.[0]).toEqual({
    mediumType: "IRIDIUM",
    mediumId: "0000",
  });
});
