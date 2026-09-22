import { describe, expect, test } from "vitest";
import { ApiOrganization } from "opendcs-api";
import { organizationTree } from "./orgHierarchy";

const org = (name: string, parent?: string): ApiOrganization =>
  ({ name, parent }) as ApiOrganization;

// A miniature of the CWMS shape: HQ at the root, divisions under it, districts
// under those. Deliberately supplied out of order.
const CWMS: ApiOrganization[] = [
  org("SPK", "SPD"),
  org("HQ"),
  org("SWD", "HQ"),
  org("SPD", "HQ"),
  org("SWT", "SWD"),
  org("MVP", "MVD"),
  org("MVD", "HQ"),
];

const shape = (
  organizations: ApiOrganization[],
  selectable?: (o: ApiOrganization) => boolean,
) =>
  organizationTree(organizations, selectable).map(
    (e) => `${"  ".repeat(e.depth)}${e.org.name}${e.selectable ? "" : " (label)"}`,
  );

describe("organizationTree", () => {
  test("nests children under parents with siblings alphabetical", () => {
    expect(shape(CWMS)).toEqual([
      "HQ",
      "  MVD",
      "    MVP",
      "  SPD",
      "    SPK",
      "  SWD",
      "    SWT",
    ]);
  });

  test("treats an office with no parent as a root", () => {
    expect(shape([org("SWT"), org("HQ"), org("MVP")])).toEqual(["HQ", "MVP", "SWT"]);
  });

  test("promotes an office whose parent is not in the list", () => {
    // The API can hand back a page of offices that omits the parent; the child
    // must still appear rather than dropping out of the menu.
    expect(shape([org("SPK", "SPD"), org("HQ")])).toEqual(["HQ", "SPK"]);
  });

  test("keeps an office that reports to itself", () => {
    expect(shape([org("HQ", "HQ")])).toEqual(["HQ"]);
  });

  test("keeps every office when report_to forms a cycle", () => {
    // Nothing in a cycle is reachable from a root, so without cycle-breaking
    // these offices would vanish entirely.
    const names = organizationTree([org("A", "B"), org("B", "A")])
      .map((e) => e.org.name)
      .sort();
    expect(names).toEqual(["A", "B"]);
  });

  test("keeps unnamed offices rather than dropping them", () => {
    const entries = organizationTree([{} as ApiOrganization, org("HQ")]);
    expect(entries).toHaveLength(2);
  });

  test("prunes to selectable offices, keeping ancestors as labels", () => {
    // The org switcher case: the user holds roles at two leaf districts only.
    const roles = new Set(["SPK", "SWT"]);
    expect(shape(CWMS, (o) => roles.has(o.name!))).toEqual([
      "HQ (label)",
      "  SPD (label)",
      "    SPK",
      "  SWD (label)",
      "    SWT",
    ]);
  });

  test("drops branches with nothing selectable in them", () => {
    const roles = new Set(["SPK"]);
    expect(shape(CWMS, (o) => roles.has(o.name!))).toEqual([
      "HQ (label)",
      "  SPD (label)",
      "    SPK",
    ]);
  });

  test("marks a selectable parent selectable even with selectable children", () => {
    const roles = new Set(["HQ", "SPK"]);
    expect(shape(CWMS, (o) => roles.has(o.name!))).toEqual([
      "HQ",
      "  SPD (label)",
      "    SPK",
    ]);
  });

  test("returns nothing when no office is selectable", () => {
    expect(organizationTree(CWMS, () => false)).toEqual([]);
  });

  test("handles an empty list", () => {
    expect(organizationTree([])).toEqual([]);
  });
});
