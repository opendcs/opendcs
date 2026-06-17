import { describe, expect, test } from "vitest";
import { QueryClient } from "@tanstack/react-query";
import { siteKeys } from "./keys";

// Behavior tests for the contract that useSaveSiteMutation /
// useDeleteSiteMutation rely on: invalidating siteKeys.all(org) must cascade
// to every list/detail query for that org and must not touch other orgs.
// These run in node without a DOM because QueryClient is plain JS.

const ORG = "acme";
const OTHER_ORG = "globex";

const seed = (client: QueryClient) => {
  client.setQueryData(siteKeys.list(ORG), [{ siteId: 1 }]);
  client.setQueryData(siteKeys.detail(ORG, 1), { siteId: 1 });
  client.setQueryData(siteKeys.detail(ORG, 2), { siteId: 2 });
  client.setQueryData(siteKeys.list(OTHER_ORG), [{ siteId: 99 }]);
  client.setQueryData(siteKeys.detail(OTHER_ORG, 99), { siteId: 99 });
};

const isStale = (client: QueryClient, key: readonly unknown[]) =>
  client.getQueryState(key)?.isInvalidated === true;

describe("siteKeys + QueryClient invalidation contract", () => {
  test("invalidating all(org) marks list and every detail for that org stale", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: siteKeys.all(ORG) });

    expect(isStale(client, siteKeys.list(ORG))).toBe(true);
    expect(isStale(client, siteKeys.detail(ORG, 1))).toBe(true);
    expect(isStale(client, siteKeys.detail(ORG, 2))).toBe(true);
  });

  test("invalidating all(org) does not affect other orgs", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: siteKeys.all(ORG) });

    expect(isStale(client, siteKeys.list(OTHER_ORG))).toBe(false);
    expect(isStale(client, siteKeys.detail(OTHER_ORG, 99))).toBe(false);
  });

  test("invalidating list(org) does not cascade to details", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: siteKeys.list(ORG) });

    expect(isStale(client, siteKeys.list(ORG))).toBe(true);
    expect(isStale(client, siteKeys.detail(ORG, 1))).toBe(false);
    expect(isStale(client, siteKeys.detail(ORG, 2))).toBe(false);
  });

  test("invalidating one detail leaves other details and the list untouched", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: siteKeys.detail(ORG, 1) });

    expect(isStale(client, siteKeys.detail(ORG, 1))).toBe(true);
    expect(isStale(client, siteKeys.detail(ORG, 2))).toBe(false);
    expect(isStale(client, siteKeys.list(ORG))).toBe(false);
  });

  test("setQueryData on detail key is recoverable by getQueryData (fetchQuery cache hit path)", () => {
    const client = new QueryClient();
    const site = { siteId: 7, publicName: "Test" };
    client.setQueryData(siteKeys.detail(ORG, 7), site);

    expect(client.getQueryData(siteKeys.detail(ORG, 7))).toEqual(site);
    expect(client.getQueryData(siteKeys.detail(OTHER_ORG, 7))).toBeUndefined();
  });
});

describe("save invalidation must refresh the imperatively-fetched detail", () => {
  // Mirrors useFetchSite: cache-hit when fresh, otherwise calls the network fn.
  const fetchDetail = async (
    client: QueryClient,
    org: string,
    siteId: number,
    network: () => Promise<{ siteId: number; publicName: string }>,
  ) =>
    client.fetchQuery({
      queryKey: siteKeys.detail(org, siteId),
      queryFn: network,
    });

  test("invalidateQueries marks the detail invalidated so fetchQuery refetches", async () => {
    const client = new QueryClient({
      defaultOptions: { queries: { staleTime: 30_000 } },
    });
    // Detail was fetched when the user opened the row (fresh by staleTime).
    client.setQueryData(siteKeys.detail(ORG, 1), {
      siteId: 1,
      publicName: "Original",
    });

    await client.invalidateQueries({ queryKey: siteKeys.all(ORG) });

    let networkCalled = false;
    const result = await fetchDetail(client, ORG, 1, async () => {
      networkCalled = true;
      return { siteId: 1, publicName: "Saved" };
    });

    // fetchQuery refetches an invalidated entry even while it is fresh by
    // staleTime, so the committed values are served.
    expect(networkCalled).toBe(true);
    expect(result.publicName).toBe("Saved");
  });

  test("removeQueries on the detail key forces fetchQuery to reload committed data", async () => {
    const client = new QueryClient({
      defaultOptions: { queries: { staleTime: 30_000 } },
    });
    client.setQueryData(siteKeys.detail(ORG, 1), {
      siteId: 1,
      publicName: "Original",
    });

    // The fix: drop the detail entry on save success.
    client.removeQueries({ queryKey: siteKeys.detail(ORG, 1) });
    await client.invalidateQueries({ queryKey: siteKeys.all(ORG) });

    let networkCalled = false;
    const result = await fetchDetail(client, ORG, 1, async () => {
      networkCalled = true;
      return { siteId: 1, publicName: "Saved" };
    });

    // FIX: cache miss → network → committed values shown.
    expect(networkCalled).toBe(true);
    expect(result.publicName).toBe("Saved");
  });
});
