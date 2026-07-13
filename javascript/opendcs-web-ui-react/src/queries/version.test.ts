import { afterEach, describe, expect, test, vi } from "vitest";
import { fetchVersion } from "./version";

describe("fetchVersion", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  test("resolves with the parsed body on a successful response", async () => {
    const version = { version: "99.main-SNAPSHOT", commitHash: "0123456" };
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(version),
      }),
    );

    await expect(fetchVersion()).resolves.toEqual(version);
    expect(fetch).toHaveBeenCalledWith("/odcsapi/version");
  });

  test("rejects with the status code when the response is not ok", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false, status: 503 }));

    await expect(fetchVersion()).rejects.toThrow("Version fetch failed: 503");
  });
});
