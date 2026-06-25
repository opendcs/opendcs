import { describe, expect, test, vi } from "vitest";

// AppDataTable does `await onSave?.(updated); setMode("show")`.
// If onSave returns void (mutate), the await resolves immediately and the row
// transitions before the API call finishes — fetching stale data on re-open.
// If onSave returns a Promise (mutateAsync), the await holds until the save
// commits, so the refetch sees fresh data.

async function simulateDetailSave(
  onSave: ((item: unknown) => void | Promise<unknown>) | undefined,
  setMode: (mode: string) => void,
): Promise<void> {
  await onSave?.({});
  setMode("show");
}

describe("Sites save: mutateAsync vs mutate timing", () => {
  test("mutate (pre-fix): setMode fires before the API call resolves", async () => {
    let resolveApiCall!: () => void;
    let apiResolved = false;
    let apiResolvedWhenModeSet: boolean | null = null;

    const onSave = vi.fn().mockImplementation(() => {
      new Promise<void>((resolve) => {
        resolveApiCall = resolve;
      }).then(() => {
        apiResolved = true;
      });
      return undefined; // void — mutate() fire-and-forget
    });

    const setMode = vi.fn().mockImplementation(() => {
      apiResolvedWhenModeSet = apiResolved;
    });

    await simulateDetailSave(onSave, setMode);

    expect(setMode).toHaveBeenCalledWith("show");
    expect(apiResolvedWhenModeSet).toBe(false); // BUG: mode changed before save finished

    resolveApiCall();
    await Promise.resolve();
    expect(apiResolved).toBe(true);
  });

  test("mutateAsync (post-fix): setMode fires only after the API call resolves", async () => {
    let resolveApiCall!: () => void;
    let apiResolved = false;
    let apiResolvedWhenModeSet: boolean | null = null;

    const onSave = vi.fn().mockImplementation(() => {
      return new Promise<void>((resolve) => {
        resolveApiCall = resolve;
      }).then(() => {
        apiResolved = true;
      });
    });

    const setMode = vi.fn().mockImplementation(() => {
      apiResolvedWhenModeSet = apiResolved;
    });

    const saveTask = simulateDetailSave(onSave, setMode);
    resolveApiCall();
    await saveTask;

    expect(setMode).toHaveBeenCalledWith("show");
    expect(apiResolvedWhenModeSet).toBe(true); // FIX: mode changed after save finished
  });
});
