import { describe, expect, test, vi } from "vitest";
import type { QueryClient } from "@tanstack/react-query";
import { normalizeNewId, removeDetailOnSave } from "./mutationHelpers";

const fakeClient = () => ({ removeQueries: vi.fn() }) as unknown as QueryClient;

describe("removeDetailOnSave", () => {
  test("removes the detail entry when the saved record has a persisted id", () => {
    const client = fakeClient();

    removeDetailOnSave(client, ["configs", "acme", "detail", 1], 1);

    expect(client.removeQueries).toHaveBeenCalledWith({
      queryKey: ["configs", "acme", "detail", 1],
    });
  });

  test("does nothing for a new record with no id yet", () => {
    const client = fakeClient();

    removeDetailOnSave(client, ["configs", "acme", "detail", -1], undefined);

    expect(client.removeQueries).not.toHaveBeenCalled();
  });

  test("does nothing for a non-positive id", () => {
    const client = fakeClient();

    removeDetailOnSave(client, ["configs", "acme", "detail", 0], 0);

    expect(client.removeQueries).not.toHaveBeenCalled();
  });

  test("does nothing for a transient negative id", () => {
    const client = fakeClient();

    removeDetailOnSave(client, ["configs", "acme", "detail", -5], -5);

    expect(client.removeQueries).not.toHaveBeenCalled();
  });
});

describe("normalizeNewId", () => {
  test("passes through a positive id", () => {
    expect(normalizeNewId(7)).toBe(7);
  });

  test("normalizes a missing or non-positive id to undefined", () => {
    expect(normalizeNewId(undefined)).toBeUndefined();
    expect(normalizeNewId(0)).toBeUndefined();
    expect(normalizeNewId(-5)).toBeUndefined();
  });
});
