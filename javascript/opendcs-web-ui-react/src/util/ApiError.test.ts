import { describe, expect, it } from "vitest";
import { ApiException } from "opendcs-api";
import { apiErrorMessage } from "./ApiError";

describe("apiErrorMessage", () => {
  const fallback = "fallback message";

  it("returns the fallback for a non-ApiException error", () => {
    expect(apiErrorMessage(new Error("boom"), fallback)).toBe(fallback);
  });

  it("returns the fallback when the ApiException has no body", () => {
    const err = new ApiException(500, "Internal Server Error", undefined, {});
    expect(apiErrorMessage(err, fallback)).toBe(fallback);
  });

  it("extracts message from an already-parsed object body", () => {
    const err = new ApiException(
      500,
      "Internal Server Error",
      { message: "duplicate key" },
      {},
    );
    expect(apiErrorMessage(err, fallback)).toBe("duplicate key");
  });

  it("extracts message from a raw JSON string body (undocumented status code)", () => {
    const err = new ApiException(
      400,
      "Unknown API Status Code!",
      JSON.stringify({ message: "duplicate key value violates unique constraint" }),
      {},
    );
    expect(apiErrorMessage(err, fallback)).toBe(
      "duplicate key value violates unique constraint",
    );
  });

  it("returns the fallback when the string body isn't valid JSON", () => {
    const err = new ApiException(502, "Bad Gateway", "<html>502</html>", {});
    expect(apiErrorMessage(err, fallback)).toBe(fallback);
  });

  it("returns the fallback when the parsed body has no message field", () => {
    const err = new ApiException(
      400,
      "Bad Request",
      JSON.stringify({ foo: "bar" }),
      {},
    );
    expect(apiErrorMessage(err, fallback)).toBe(fallback);
  });

  it("returns the fallback when message is blank", () => {
    const err = new ApiException(400, "Bad Request", { message: "   " }, {});
    expect(apiErrorMessage(err, fallback)).toBe(fallback);
  });
});
