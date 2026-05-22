import { describe, expect, test } from "vitest";
import { algorithmKeys, intervalKeys, platformKeys, siteKeys } from "./keys";

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

describe("algorithmKeys", () => {
  test("all() scopes the key to an org", () => {
    expect(algorithmKeys.all("acme")).toEqual(["algorithms", "acme"]);
    expect(algorithmKeys.all("acme")).not.toEqual(algorithmKeys.all("globex"));
  });

  test("list() extends all() so invalidating all() also invalidates list()", () => {
    const all = algorithmKeys.all("acme");
    const list = algorithmKeys.list("acme");
    expect(list.slice(0, all.length)).toEqual([...all]);
    expect(list).toEqual(["algorithms", "acme", "list"]);
  });

  test("detail() extends all() and includes the algorithm id", () => {
    const all = algorithmKeys.all("acme");
    const detail = algorithmKeys.detail("acme", 42);
    expect(detail.slice(0, all.length)).toEqual([...all]);
    expect(detail).toEqual(["algorithms", "acme", "detail", 42]);
  });

  test("propSpecs() extends all() and includes the exec class", () => {
    const all = algorithmKeys.all("acme");
    const spec = algorithmKeys.propSpecs("acme", "org.example.MyAlgo");
    expect(spec.slice(0, all.length)).toEqual([...all]);
    expect(spec).toEqual(["algorithms", "acme", "propSpecs", "org.example.MyAlgo"]);
  });

  test("catalog() extends all() and is scoped to org", () => {
    const all = algorithmKeys.all("acme");
    const catalog = algorithmKeys.catalog("acme");
    expect(catalog.slice(0, all.length)).toEqual([...all]);
    expect(catalog).toEqual(["algorithms", "acme", "catalog"]);
  });

  test("keys for different orgs never collide", () => {
    expect(algorithmKeys.list("acme")).not.toEqual(algorithmKeys.list("globex"));
    expect(algorithmKeys.detail("acme", 1)).not.toEqual(
      algorithmKeys.detail("globex", 1),
    );
    expect(algorithmKeys.propSpecs("acme", "X")).not.toEqual(
      algorithmKeys.propSpecs("globex", "X"),
    );
    expect(algorithmKeys.catalog("acme")).not.toEqual(algorithmKeys.catalog("globex"));
  });
});

describe("intervalKeys", () => {
  test("all() scopes the key to an org", () => {
    expect(intervalKeys.all("acme")).toEqual(["intervals", "acme"]);
    expect(intervalKeys.all("acme")).not.toEqual(intervalKeys.all("globex"));
  });

  test("list() extends all() so invalidating all() also invalidates list()", () => {
    const all = intervalKeys.all("acme");
    const list = intervalKeys.list("acme");
    expect(list.slice(0, all.length)).toEqual([...all]);
    expect(list).toEqual(["intervals", "acme", "list"]);
  });

  test("keys for different orgs never collide", () => {
    expect(intervalKeys.list("acme")).not.toEqual(intervalKeys.list("globex"));
  });
});

describe("platformKeys", () => {
  test("all() scopes the key to an org", () => {
    expect(platformKeys.all("acme")).toEqual(["platforms", "acme"]);
    expect(platformKeys.all("acme")).not.toEqual(platformKeys.all("globex"));
  });

  test("list() extends all() so invalidating all() also invalidates list()", () => {
    const all = platformKeys.all("acme");
    const list = platformKeys.list("acme");
    expect(list.slice(0, all.length)).toEqual([...all]);
    expect(list).toEqual(["platforms", "acme", "list"]);
  });

  test("detail() extends all() and includes the platform id", () => {
    const all = platformKeys.all("acme");
    const detail = platformKeys.detail("acme", 42);
    expect(detail.slice(0, all.length)).toEqual([...all]);
    expect(detail).toEqual(["platforms", "acme", "detail", 42]);
  });

  test("config() extends all() and includes the config id", () => {
    const all = platformKeys.all("acme");
    const config = platformKeys.config("acme", 7);
    expect(config.slice(0, all.length)).toEqual([...all]);
    expect(config).toEqual(["platforms", "acme", "config", 7]);
  });

  test("keys for different orgs never collide", () => {
    expect(platformKeys.list("acme")).not.toEqual(platformKeys.list("globex"));
    expect(platformKeys.detail("acme", 1)).not.toEqual(
      platformKeys.detail("globex", 1),
    );
    expect(platformKeys.config("acme", 1)).not.toEqual(
      platformKeys.config("globex", 1),
    );
  });
});
