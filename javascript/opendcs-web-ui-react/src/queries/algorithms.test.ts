import { describe, expect, test } from "vitest";
import { QueryClient } from "@tanstack/react-query";
import { algorithmKeys } from "./keys";

// Verifies the invalidation contract useImportAlgorithmsMutation.onSuccess
// relies on: a single invalidateQueries(algorithmKeys.all(org)) must stale the
// list, all details, AND the catalog — so the table and "Check for New" modal
// both reflect just-imported records without a page reload.

const ORG = "acme";
const OTHER_ORG = "globex";

const seed = (client: QueryClient) => {
  client.setQueryData(algorithmKeys.list(ORG), [{ algorithmId: 1 }]);
  client.setQueryData(algorithmKeys.detail(ORG, 1), { algorithmId: 1 });
  client.setQueryData(algorithmKeys.catalog(ORG), []);
  client.setQueryData(algorithmKeys.list(OTHER_ORG), [{ algorithmId: 99 }]);
  client.setQueryData(algorithmKeys.catalog(OTHER_ORG), []);
};

const isStale = (c: QueryClient, key: readonly unknown[]) =>
  c.getQueryState(key)?.isInvalidated === true;

describe("algorithmKeys invalidation contract", () => {
  test("all(org) cascades to list, detail, and catalog for that org", () => {
    const c = new QueryClient();
    seed(c);
    c.invalidateQueries({ queryKey: algorithmKeys.all(ORG) });

    expect(isStale(c, algorithmKeys.list(ORG))).toBe(true);
    expect(isStale(c, algorithmKeys.detail(ORG, 1))).toBe(true);
    expect(isStale(c, algorithmKeys.catalog(ORG))).toBe(true);
  });

  test("all(org) does not stale other orgs", () => {
    const c = new QueryClient();
    seed(c);
    c.invalidateQueries({ queryKey: algorithmKeys.all(ORG) });

    expect(isStale(c, algorithmKeys.list(OTHER_ORG))).toBe(false);
    expect(isStale(c, algorithmKeys.catalog(OTHER_ORG))).toBe(false);
  });
});
