import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { getToasts, pushToast, subscribeToasts } from "./toastBus";

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  // Drain any pending expiry timers so state doesn't leak into other tests.
  vi.runOnlyPendingTimers();
  vi.useRealTimers();
});

describe("toastBus", () => {
  test("pushToast adds a toast and notifies subscribers", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeToasts(listener);

    const before = getToasts().length;
    pushToast("Saved successfully", "success");

    expect(listener).toHaveBeenCalledTimes(1);
    const toasts = getToasts();
    expect(toasts.length).toEqual(before + 1);
    const added = toasts[toasts.length - 1];
    expect(added.message).toEqual("Saved successfully");
    expect(added.variant).toEqual("success");
    expect(added.ttlMs).toEqual(5000);

    unsubscribe();
  });

  test("danger toasts default to an 8000ms ttl, others default to 5000ms", () => {
    pushToast("Something failed", "danger");
    const toasts = getToasts();
    expect(toasts[toasts.length - 1].ttlMs).toEqual(8000);
  });

  test("an explicit ttlMs overrides the variant default", () => {
    pushToast("Custom ttl", "info", 1234);
    const toasts = getToasts();
    expect(toasts[toasts.length - 1].ttlMs).toEqual(1234);
  });

  test("a toast removes itself after its ttl elapses and notifies again", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeToasts(listener);

    pushToast("Expiring toast", "info", 1000);
    const id = getToasts()[getToasts().length - 1].id;
    expect(getToasts().some((t) => t.id === id)).toBe(true);

    vi.advanceTimersByTime(1000);

    expect(getToasts().some((t) => t.id === id)).toBe(false);
    expect(listener.mock.calls.length).toBeGreaterThanOrEqual(2);

    unsubscribe();
  });

  test("unsubscribe stops further notifications", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeToasts(listener);
    unsubscribe();

    pushToast("After unsubscribe", "info", 1000);
    expect(listener).not.toHaveBeenCalled();
  });
});
