import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { dismissToast, getToasts, pushToast, subscribeToasts } from "./toastBus";

describe("toastBus", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    // Let every pending expiry fire so toasts don't leak into the next test.
    vi.runAllTimers();
    vi.useRealTimers();
  });

  const last = () => getToasts()[getToasts().length - 1];

  it("adds a toast and notifies subscribers", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeToasts(listener);

    pushToast({ variant: "success", title: "translation:delete_success" });

    expect(listener).toHaveBeenCalledTimes(1);
    expect(last()).toMatchObject({
      variant: "success",
      title: "translation:delete_success",
    });
    unsubscribe();
  });

  it("expires success toasts after 5s and error toasts after 8s", () => {
    pushToast({ variant: "success", title: "ok" });
    const okId = last().id;
    pushToast({ variant: "danger", title: "failed", detail: "in use" });
    const failedId = last().id;

    vi.advanceTimersByTime(5000);
    expect(getToasts().map((t) => t.id)).toEqual([failedId]);
    expect(getToasts().some((t) => t.id === okId)).toBe(false);

    vi.advanceTimersByTime(3000);
    expect(getToasts()).toHaveLength(0);
  });

  it("dismisses a toast early and ignores a second dismiss", () => {
    const listener = vi.fn();
    pushToast({ variant: "danger", title: "failed" });
    const id = last().id;
    const unsubscribe = subscribeToasts(listener);

    dismissToast(id);
    dismissToast(id);

    expect(getToasts()).toHaveLength(0);
    expect(listener).toHaveBeenCalledTimes(1);
    unsubscribe();
  });

  it("stops notifying after unsubscribe", () => {
    const listener = vi.fn();
    subscribeToasts(listener)();

    pushToast({ variant: "success", title: "ok" });

    expect(listener).not.toHaveBeenCalled();
  });
});
