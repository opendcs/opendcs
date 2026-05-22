import { describe, expect, test } from "vitest";
import { QueryClient } from "@tanstack/react-query";
import { appKeys } from "./keys";

// Behavior tests for the contract that useSaveAppMutation /
// useDeleteAppMutation rely on: invalidating appKeys.all(org) must cascade
// to every list/detail query for that org and must not touch other orgs.
// These run in node without a DOM because QueryClient is plain JS.

const ORG = "acme";
const OTHER_ORG = "globex";

const detailKey = (org: string, appId: number) =>
  [...appKeys.all(org), "detail", appId] as const;

const seed = (client: QueryClient) => {
  client.setQueryData(appKeys.list(ORG), [{ appId: 1 }]);
  client.setQueryData(detailKey(ORG, 1), { appId: 1, appName: "compproc" });
  client.setQueryData(detailKey(ORG, 2), { appId: 2, appName: "routing" });
  client.setQueryData(appKeys.list(OTHER_ORG), [{ appId: 99 }]);
  client.setQueryData(detailKey(OTHER_ORG, 99), { appId: 99 });
};

const isStale = (client: QueryClient, key: readonly unknown[]) =>
  client.getQueryState(key)?.isInvalidated === true;

describe("appKeys + QueryClient invalidation contract", () => {
  test("invalidating all(org) marks list and every detail for that org stale", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: appKeys.all(ORG) });

    expect(isStale(client, appKeys.list(ORG))).toBe(true);
    expect(isStale(client, detailKey(ORG, 1))).toBe(true);
    expect(isStale(client, detailKey(ORG, 2))).toBe(true);
  });

  test("invalidating all(org) does not affect other orgs", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: appKeys.all(ORG) });

    expect(isStale(client, appKeys.list(OTHER_ORG))).toBe(false);
    expect(isStale(client, detailKey(OTHER_ORG, 99))).toBe(false);
  });

  test("invalidating list(org) does not cascade to cached details", () => {
    const client = new QueryClient();
    seed(client);

    client.invalidateQueries({ queryKey: appKeys.list(ORG) });

    expect(isStale(client, appKeys.list(ORG))).toBe(true);
    expect(isStale(client, detailKey(ORG, 1))).toBe(false);
    expect(isStale(client, detailKey(ORG, 2))).toBe(false);
  });

  test("setQueryData on detail key is recoverable (useFetchApp cache-hit path)", () => {
    const client = new QueryClient();
    const app = { appId: 5, appName: "compproc" };
    client.setQueryData(detailKey(ORG, 5), app);

    expect(client.getQueryData(detailKey(ORG, 5))).toEqual(app);
    expect(client.getQueryData(detailKey(OTHER_ORG, 5))).toBeUndefined();
  });
});
