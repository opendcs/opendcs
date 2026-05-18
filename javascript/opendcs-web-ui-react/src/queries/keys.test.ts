import { describe, expect, test } from "vitest";
import { siteKeys } from "./keys";

describe("siteKeys", () => {
  test("all() scopes the key to an org", () => {
    expect(siteKeys.all("acme")).toEqual(["sites", "acme"]);
    expect(siteKeys.all("acme")).not.toEqual(siteKeys.all("globex"));
  });

  test("list() extends all() so invalidating all() also invalidates list()", () => {
    const all = siteKeys.all("acme");
    const list = siteKeys.list("acme");
    expect(list.slice(0, all.length)).toEqual([...all]);
    expect(list).toEqual(["sites", "acme", "list"]);
  });

  test("detail() extends all() and includes the site id", () => {
    const all = siteKeys.all("acme");
    const detail = siteKeys.detail("acme", 42);
    expect(detail.slice(0, all.length)).toEqual([...all]);
    expect(detail).toEqual(["sites", "acme", "detail", 42]);
  });

  test("detail() keys differ per site id within the same org", () => {
    expect(siteKeys.detail("acme", 1)).not.toEqual(siteKeys.detail("acme", 2));
  });

  test("keys for different orgs never collide", () => {
    expect(siteKeys.list("acme")).not.toEqual(siteKeys.list("globex"));
    expect(siteKeys.detail("acme", 1)).not.toEqual(siteKeys.detail("globex", 1));
  });
});
