import { describe, expect, test } from "vitest";
import { QueryClient } from "@tanstack/react-query";
import { configKeys } from "./keys";

const ORG = "acme";
const OTHER_ORG = "globex";

const seed = (client: QueryClient) => {
  client.setQueryData(configKeys.list(ORG), [{ configId: 1 }]);
  client.setQueryData(configKeys.detail(ORG, 1), { configId: 1 });
  client.setQueryData(configKeys.detail(ORG, 2), { configId: 2 });
  client.setQueryData(configKeys.list(OTHER_ORG), [{ configId: 99 }]);
  client.setQueryData(configKeys.detail(OTHER_ORG, 99), { configId: 99 });
};

const isStale = (client: QueryClient, key: readonly unknown[]) =>
  client.getQueryState(key)?.isInvalidated === true;

describe("configKeys + QueryClient invalidation contract", () => {
  test("invalidating all(org) marks list and every detail for that org stale", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: configKeys.all(ORG) });

    expect(isStale(client, configKeys.list(ORG))).toBe(true);
    expect(isStale(client, configKeys.detail(ORG, 1))).toBe(true);
    expect(isStale(client, configKeys.detail(ORG, 2))).toBe(true);
  });

  test("invalidating all(org) does not affect other orgs", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: configKeys.all(ORG) });

    expect(isStale(client, configKeys.list(OTHER_ORG))).toBe(false);
    expect(isStale(client, configKeys.detail(OTHER_ORG, 99))).toBe(false);
  });

  test("invalidating list(org) does not cascade to details", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: configKeys.list(ORG) });

    expect(isStale(client, configKeys.list(ORG))).toBe(true);
    expect(isStale(client, configKeys.detail(ORG, 1))).toBe(false);
    expect(isStale(client, configKeys.detail(ORG, 2))).toBe(false);
  });

  test("invalidating one detail leaves other details and the list untouched", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: configKeys.detail(ORG, 1) });

    expect(isStale(client, configKeys.detail(ORG, 1))).toBe(true);
    expect(isStale(client, configKeys.detail(ORG, 2))).toBe(false);
    expect(isStale(client, configKeys.list(ORG))).toBe(false);
  });
});

describe("save invalidation must refresh the imperatively-fetched detail", () => {
  // Mirrors useFetchConfig: cache-hit when fresh, otherwise calls the network fn.
  const fetchDetail = async (
    client: QueryClient,
    org: string,
    configId: number,
    network: () => Promise<{ configId: number; name: string }>,
  ) =>
    client.fetchQuery({
      queryKey: configKeys.detail(org, configId),
      queryFn: network,
    });

  test("removeQueries on the detail key forces fetchQuery to reload committed data", async () => {
    const client = new QueryClient({
      defaultOptions: { queries: { staleTime: 30_000 } },
    });
    // Detail was fetched when the user opened the row (fresh by staleTime),
    // and holds the values as they were before an edit.
    client.setQueryData(configKeys.detail(ORG, 1), {
      configId: 1,
      name: "Original",
    });

    client.removeQueries({ queryKey: configKeys.detail(ORG, 1) });
    await client.invalidateQueries({ queryKey: configKeys.all(ORG) });

    let networkCalled = false;
    const result = await fetchDetail(client, ORG, 1, async () => {
      networkCalled = true;
      return { configId: 1, name: "Saved" };
    });

    expect(networkCalled).toBe(true);
    expect(result.name).toBe("Saved");
  });
});
