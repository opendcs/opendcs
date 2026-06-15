import { describe, expect, test } from "vitest";
import {
  algorithmKeys,
  appKeys,
  configKeys,
  intervalKeys,
  platformKeys,
  siteKeys,
} from "./keys";

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

  test("keys for different orgs never collide", () => {
    expect(platformKeys.list("acme")).not.toEqual(platformKeys.list("globex"));
    expect(platformKeys.detail("acme", 1)).not.toEqual(
      platformKeys.detail("globex", 1),
    );
  });
});

describe("configKeys", () => {
  test("all() scopes the key to an org", () => {
    expect(configKeys.all("acme")).toEqual(["configs", "acme"]);
    expect(configKeys.all("acme")).not.toEqual(configKeys.all("globex"));
  });

  test("list() extends all() so invalidating all() also invalidates list()", () => {
    const all = configKeys.all("acme");
    const list = configKeys.list("acme");
    expect(list.slice(0, all.length)).toEqual([...all]);
    expect(list).toEqual(["configs", "acme", "list"]);
  });

  test("detail() extends all() and includes the config id", () => {
    const all = configKeys.all("acme");
    const detail = configKeys.detail("acme", 7);
    expect(detail.slice(0, all.length)).toEqual([...all]);
    expect(detail).toEqual(["configs", "acme", "detail", 7]);
  });

  test("keys for different orgs never collide", () => {
    expect(configKeys.list("acme")).not.toEqual(configKeys.list("globex"));
    expect(configKeys.detail("acme", 1)).not.toEqual(configKeys.detail("globex", 1));
  });
});

describe("appKeys", () => {
  test("all() scopes the key to an org", () => {
    expect(appKeys.all("acme")).toEqual(["apps", "acme"]);
    expect(appKeys.all("acme")).not.toEqual(appKeys.all("globex"));
  });

  test("list() extends all() so invalidating all() also invalidates list()", () => {
    const all = appKeys.all("acme");
    const list = appKeys.list("acme");
    expect(list.slice(0, all.length)).toEqual([...all]);
    expect(list).toEqual(["apps", "acme", "list"]);
  });

  test("stat() extends all() so invalidating all() also invalidates stat()", () => {
    const all = appKeys.all("acme");
    const stat = appKeys.stat("acme");
    expect(stat.slice(0, all.length)).toEqual([...all]);
    expect(stat).toEqual(["apps", "acme", "stat"]);
  });

  test("keys for different orgs never collide", () => {
    expect(appKeys.list("acme")).not.toEqual(appKeys.list("globex"));
    expect(appKeys.stat("acme")).not.toEqual(appKeys.stat("globex"));
  });
});
