import { describe, expect, it } from "vitest";
import { ComputationReducer } from "./ComputationReducer";
import type { UiComputation } from "./Computation";

const baseComputation: UiComputation = Object.freeze({
  computationId: 1,
  name: "DailyFlowAve",
  algorithmId: 10,
  algorithmName: "AverageAlgorithm",
  props: { existing: "value" },
});

describe("ComputationReducer", () => {
  describe("save_prop", () => {
    it.each([
      {
        scenario: "adds a new property",
        initial: { existing: "value" },
        payload: { name: "newProp", value: "42" },
        expected: { existing: "value", newProp: "42" },
      },
      {
        scenario: "overwrites an existing value",
        initial: { existing: "value" },
        payload: { name: "existing", value: "updated" },
        expected: { existing: "updated" },
      },
      {
        scenario: "starts a fresh map when initial props are undefined",
        initial: undefined,
        payload: { name: "first", value: "1" },
        expected: { first: "1" },
      },
    ])("$scenario", ({ initial, payload, expected }) => {
      const result = ComputationReducer(
        { ...baseComputation, props: initial },
        { type: "save_prop", payload },
      );
      expect(result.props).toEqual(expected);
    });
  });

  describe("delete_prop", () => {
    it("removes a property by name", () => {
      const result = ComputationReducer(
        { ...baseComputation, props: { keep: "1", drop: "2" } },
        { type: "delete_prop", payload: { name: "drop" } },
      );

      expect(result.props).toEqual({ keep: "1" });
    });

    it("is a no-op when the property does not exist", () => {
      const result = ComputationReducer(baseComputation, {
        type: "delete_prop",
        payload: { name: "missing" },
      });

      expect(result.props).toEqual({ existing: "value" });
    });
  });

  describe("save", () => {
    it("merges payload fields over current state", () => {
      const result = ComputationReducer(baseComputation, {
        type: "save",
        payload: { name: "Renamed", comment: "new comment" },
      });

      expect(result.name).toBe("Renamed");
      expect(result.comment).toBe("new comment");
      expect(result.computationId).toBe(1);
    });

    it("can replace props entirely when provided in the payload", () => {
      const result = ComputationReducer(baseComputation, {
        type: "save",
        payload: { props: { newOnly: "1" } },
      });

      expect(result.props).toEqual({ newOnly: "1" });
    });
  });

  describe("merge_algo_props", () => {
    it("adds new algorithm defaults but keeps existing user values", () => {
      const result = ComputationReducer(baseComputation, {
        type: "merge_algo_props",
        payload: { existing: "default", brandNew: "fromAlgo" },
      });

      expect(result.props).toEqual({
        existing: "value",
        brandNew: "fromAlgo",
      });
    });

    it("works when the current computation has no props", () => {
      const result = ComputationReducer(
        { ...baseComputation, props: undefined },
        { type: "merge_algo_props", payload: { a: "1", b: "2" } },
      );

      expect(result.props).toEqual({ a: "1", b: "2" });
    });
  });
});
