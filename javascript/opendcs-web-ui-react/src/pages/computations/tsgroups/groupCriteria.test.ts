import { expect, test } from "vitest";
import {
  criteriaRows,
  dataTypeLabel,
  formatGroupAttr,
  parseGroupAttr,
  siteCriterionLabel,
  splitBaseSub,
  tsidParts,
} from "./groupCriteria";

test("parseGroupAttr splits on the first '=' only", () => {
  expect(parseGroupAttr("Interval=1Hour")).toEqual({
    name: "Interval",
    value: "1Hour",
  });
  expect(parseGroupAttr("Version=a=b")).toEqual({ name: "Version", value: "a=b" });
});

test("parseGroupAttr tolerates an attribute with no value", () => {
  expect(parseGroupAttr("Duration")).toEqual({ name: "Duration", value: "" });
});

test("formatGroupAttr round-trips through parseGroupAttr", () => {
  const raw = formatGroupAttr("SubLocation", "Spillway*-Gate*");
  expect(parseGroupAttr(raw)).toEqual({
    name: "SubLocation",
    value: "Spillway*-Gate*",
  });
});

test("splitBaseSub splits on the first hyphen", () => {
  expect(splitBaseSub("ABC-Spillway1-Gate1")).toEqual({
    base: "ABC",
    sub: "Spillway1-Gate1",
  });
});

test("splitBaseSub treats a hyphen-free value as all base", () => {
  expect(splitBaseSub("OKVI4")).toEqual({ base: "OKVI4", sub: "" });
});

test("tsidParts breaks a six-part CWMS identifier into its columns", () => {
  expect(tsidParts("OKVI4.Stage.Inst.15Minutes.0.raw")).toEqual({
    location: "OKVI4",
    param: "Stage",
    paramType: "Inst",
    interval: "15Minutes",
    duration: "0",
    version: "raw",
  });
});

test("tsidParts yields empty parts for identifiers that aren't six parts", () => {
  expect(tsidParts("SITE.HG.Hour").location).toEqual("");
  expect(tsidParts(undefined).version).toEqual("");
});

test("siteCriterionLabel prefers the requested name type", () => {
  const site = { siteId: 2, sitenames: { CWMS: "ROWI4", USGS: "05449500" } };
  expect(siteCriterionLabel(site, "USGS")).toEqual("05449500");
});

test("siteCriterionLabel falls back to any name, then the public name", () => {
  const site = { siteId: 2, sitenames: { CWMS: "ROWI4" } };
  expect(siteCriterionLabel(site, "USGS")).toEqual("ROWI4");
  expect(siteCriterionLabel({ siteId: 3, publicName: "Barre Falls" }, "")).toEqual(
    "Barre Falls",
  );
});

test("dataTypeLabel falls back to standard:code without a display name", () => {
  expect(dataTypeLabel({ id: 1, standard: "CWMS", code: "Stage" })).toEqual(
    "CWMS:Stage",
  );
});

test("criteriaRows flattens sites, data types and attributes into one list", () => {
  const rows = criteriaRows(
    {
      groupSites: [{ siteId: 2, sitenames: { CWMS: "ROWI4" } }],
      groupDataTypes: [{ id: 5, standard: "CWMS", code: "Stage" }],
      groupAttrs: ["Interval=1Hour", "SubLocation=Gate*"],
    },
    (site) => siteCriterionLabel(site, "CWMS"),
  );

  expect(rows.map((r) => [r.part, r.value])).toEqual([
    ["Location", "ROWI4"],
    ["Param", "CWMS:Stage"],
    ["Interval", "1Hour"],
    ["SubLocation", "Gate*"],
  ]);
  expect(new Set(rows.map((r) => r.key)).size).toEqual(4);
});
