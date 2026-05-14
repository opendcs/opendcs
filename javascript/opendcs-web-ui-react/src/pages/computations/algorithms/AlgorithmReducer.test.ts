import { expect, test } from "vitest";
import type { UiAlgorithm } from "./Algorithm";
import { AlgorithmReducer } from "./AlgorithmReducer";

test("save_prop adds a new property when none exist", () => {
  const algorithm: UiAlgorithm = {};

  const result = AlgorithmReducer(algorithm, {
    type: "save_prop",
    payload: { name: "prop1", value: "value1" },
  });

  expect(result.props).toBeDefined();
  expect(result.props?.prop1).toEqual("value1");
});

test("save_prop overwrites an existing property and preserves others", () => {
  const algorithm: UiAlgorithm = {
    props: { prop1: "old", prop2: "keep" },
  };

  const result = AlgorithmReducer(algorithm, {
    type: "save_prop",
    payload: { name: "prop1", value: "new" },
  });

  expect(result.props?.prop1).toEqual("new");
  expect(result.props?.prop2).toEqual("keep");
});

test("delete_prop removes the property and keeps the rest", () => {
  const algorithm: UiAlgorithm = {
    props: { prop1: "value1", prop2: "value2" },
  };

  const result = AlgorithmReducer(algorithm, {
    type: "delete_prop",
    payload: { name: "prop1" },
  });

  expect(result.props?.prop1).toBeUndefined();
  expect(result.props?.prop2).toEqual("value2");
});

test("save merges payload into the existing algorithm", () => {
  const algorithm: UiAlgorithm = { name: "alg1", description: "old" };

  const result = AlgorithmReducer(algorithm, {
    type: "save",
    payload: { description: "new", execClass: "decodes.comp.MaxToDate" },
  });

  expect(result.name).toEqual("alg1");
  expect(result.description).toEqual("new");
  expect(result.execClass).toEqual("decodes.comp.MaxToDate");
});
