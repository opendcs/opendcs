import { describe, it, expect } from "vitest";
import { ApiException } from "opendcs-api";
import { saveErrorMessage } from "./saveErrorMessage";

const FALLBACK = "Save failed.";

const apiException = (body: unknown) => new ApiException(400, "Bad Request", body, {});

describe("saveErrorMessage", () => {
  it("returns the message from an ApiException body", () => {
    expect(
      saveErrorMessage(apiException({ message: "Site is required" }), FALLBACK),
    ).toBe("Site is required");
  });

  it("falls back when the error is not an ApiException", () => {
    expect(saveErrorMessage(new Error("boom"), FALLBACK)).toBe(FALLBACK);
  });

  it("falls back for a non-Error value", () => {
    expect(saveErrorMessage("boom", FALLBACK)).toBe(FALLBACK);
  });

  it("falls back when the body is null", () => {
    expect(saveErrorMessage(apiException(null), FALLBACK)).toBe(FALLBACK);
  });

  it("falls back when the body is not an object", () => {
    expect(saveErrorMessage(apiException("plain text body"), FALLBACK)).toBe(FALLBACK);
  });

  it("falls back when the body has no message", () => {
    expect(saveErrorMessage(apiException({ errorCode: 400 }), FALLBACK)).toBe(FALLBACK);
  });

  it("falls back when the message is not a string", () => {
    expect(saveErrorMessage(apiException({ message: 42 }), FALLBACK)).toBe(FALLBACK);
  });

  it("falls back when the message is blank", () => {
    expect(saveErrorMessage(apiException({ message: "   " }), FALLBACK)).toBe(FALLBACK);
  });
});
