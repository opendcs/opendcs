import { describe, expect, test } from "vitest";
import { QueryClient } from "@tanstack/react-query";
import { platformKeys } from "./keys";

// Behavior tests for the contract that useSavePlatformMutation /
// useDeletePlatformMutation rely on: invalidating platformKeys.all(org) must
// cascade to every list/detail query for that org and must not touch other orgs.
// These run in node without a DOM because QueryClient is plain JS.

const ORG = "acme";
const OTHER_ORG = "globex";

const seed = (client: QueryClient) => {
  client.setQueryData(platformKeys.list(ORG), [{ platformId: 1 }]);
  client.setQueryData(platformKeys.detail(ORG, 1), { platformId: 1 });
  client.setQueryData(platformKeys.detail(ORG, 2), { platformId: 2 });
  client.setQueryData(platformKeys.list(OTHER_ORG), [{ platformId: 99 }]);
  client.setQueryData(platformKeys.detail(OTHER_ORG, 99), { platformId: 99 });
};

const isStale = (client: QueryClient, key: readonly unknown[]) =>
  client.getQueryState(key)?.isInvalidated === true;

describe("platformKeys + QueryClient invalidation contract", () => {
  test("invalidating all(org) marks list and every detail for that org stale", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: platformKeys.all(ORG) });

    expect(isStale(client, platformKeys.list(ORG))).toBe(true);
    expect(isStale(client, platformKeys.detail(ORG, 1))).toBe(true);
    expect(isStale(client, platformKeys.detail(ORG, 2))).toBe(true);
  });

  test("invalidating all(org) does not affect other orgs", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: platformKeys.all(ORG) });

    expect(isStale(client, platformKeys.list(OTHER_ORG))).toBe(false);
    expect(isStale(client, platformKeys.detail(OTHER_ORG, 99))).toBe(false);
  });

  test("invalidating list(org) does not cascade to cached details", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: platformKeys.list(ORG) });

    expect(isStale(client, platformKeys.list(ORG))).toBe(true);
    expect(isStale(client, platformKeys.detail(ORG, 1))).toBe(false);
    expect(isStale(client, platformKeys.detail(ORG, 2))).toBe(false);
  });

  test("setQueryData on detail key is recoverable (useFetchPlatform cache-hit path)", () => {
    const client = new QueryClient();
    const platform = { platformId: 7, description: "Test Platform" };
    client.setQueryData(platformKeys.detail(ORG, 7), platform);

    expect(client.getQueryData(platformKeys.detail(ORG, 7))).toEqual(platform);
    expect(client.getQueryData(platformKeys.detail(OTHER_ORG, 7))).toBeUndefined();
  });
});
