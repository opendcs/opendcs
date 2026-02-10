import { expect, test } from "vitest";
import { PropertiesReducer } from "./PropertiesReducer";
import type { Property } from "./Properties";

test("Reducer Add Proper", () => {
  const testProps: Property[] = [];

  const result = PropertiesReducer(testProps, {
    type: "save_prop",
    payload: { name: "prop1", value: "value" },
  });
  console.log(result);
  expect(result).toBeDefined();

  expect(result[0]).toEqual("value");
});

test("Change a Prop Then Delete", () => {
  const testProps: Property[] = [
    { name: "prop1", value: "value1" },
    { name: "prop2", value: "value2" },
  ];

  const result = PropertiesReducer(testProps, {
    type: "save_prop",
    payload: { name: "prop1", value: "Test Rename" },
  });
  expect(result).toBeDefined();
  expect(result[0]).toEqual("Test Rename");

  const deletedName = PropertiesReducer(result, {
    type: "delete_prop",
    payload: { name: "prop2" },
  });
  expect(deletedName.length).toEqual(1);
});
