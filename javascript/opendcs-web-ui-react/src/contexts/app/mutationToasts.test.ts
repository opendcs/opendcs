import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { Mutation } from "@tanstack/react-query";
import { ApiException } from "opendcs-api";
import { getToasts } from "../../util/toastBus";
import {
  DELETE_TOASTS,
  onMutationError,
  onMutationSuccess,
  type ToastMeta,
} from "./mutationToasts";

const mutationWith = (meta?: ToastMeta) =>
  ({ meta }) as unknown as Mutation<unknown, unknown, unknown, unknown>;

describe("mutationToasts", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.runAllTimers();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("shows a success toast for a mutation that opted in", () => {
    onMutationSuccess(undefined, 1, undefined, mutationWith(DELETE_TOASTS));

    expect(getToasts()).toEqual([
      expect.objectContaining({
        variant: "success",
        title: "translation:delete_success",
      }),
    ]);
  });

  it("shows the server's message under the error headline", () => {
    const err = new ApiException(
      409,
      "Site is in use and cannot be deleted",
      { message: "Cannot perform operation because the data is in use." },
      {},
    );

    onMutationError(err, 1, undefined, mutationWith(DELETE_TOASTS));

    expect(getToasts()).toEqual([
      expect.objectContaining({
        variant: "danger",
        title: "translation:delete_failed",
        detail: "Cannot perform operation because the data is in use.",
      }),
    ]);
  });

  it("shows only the headline when the error carries no message", () => {
    onMutationError(new Error("network"), 1, undefined, mutationWith(DELETE_TOASTS));

    expect(getToasts()).toEqual([
      expect.objectContaining({ variant: "danger", detail: undefined }),
    ]);
  });

  it("stays silent for mutations without toast meta", () => {
    onMutationSuccess(undefined, 1, undefined, mutationWith());
    onMutationError(new Error("boom"), 1, undefined, mutationWith());

    expect(getToasts()).toHaveLength(0);
    expect(console.error).toHaveBeenCalledWith("[mutation]", expect.any(Error));
  });
});
