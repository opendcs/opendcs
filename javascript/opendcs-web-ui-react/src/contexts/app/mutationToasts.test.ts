import { describe, expect, test } from "vitest";
import { extractErrorMessage } from "./mutationToasts";

describe("extractErrorMessage", () => {
  test("prefers body.message from a parsed Status JSON response", () => {
    const error = { body: { message: "Config name already exists" } };
    expect(extractErrorMessage(error)).toEqual("Config name already exists");
  });

  test("ignores body when body.message is not a string", () => {
    const error = { body: { message: 42 } };
    expect(extractErrorMessage(error)).toEqual("An unexpected error occurred.");
  });

  test("extracts the Message: line from an ApiException-style dump", () => {
    const error = new Error(
      "HTTP-Code: 409\nMessage: Config in use by 3 platforms\nURL: /config",
    );
    expect(extractErrorMessage(error)).toEqual("Config in use by 3 platforms");
  });

  test("falls back to the full Error message when there is no Message: line", () => {
    const error = new Error("Network request failed");
    expect(extractErrorMessage(error)).toEqual("Network request failed");
  });

  test("falls back to a generic message for non-Error, non-body values", () => {
    expect(extractErrorMessage("just a string")).toEqual(
      "An unexpected error occurred.",
    );
    expect(extractErrorMessage(null)).toEqual("An unexpected error occurred.");
    expect(extractErrorMessage(undefined)).toEqual("An unexpected error occurred.");
  });
});
