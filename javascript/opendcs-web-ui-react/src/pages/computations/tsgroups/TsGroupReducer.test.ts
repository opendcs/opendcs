import { expect, test } from "vitest";
import { TsGroupReducer, combineField, type UiTsGroup } from "./TsGroupReducer";
import { attrCriterionKey, criteriaRows, siteCriterionKey } from "./groupCriteria";

const siteLabel = () => "OKVI4";

test("save merges the payload into the existing group", () => {
  const group: UiTsGroup = { groupName: "basin", description: "old" };

  const result = TsGroupReducer(group, {
    type: "save",
    payload: { description: "new" },
  });

  expect(result.groupName).toEqual("basin");
  expect(result.description).toEqual("new");
});

test("add_ts_members appends and ignores time series already in the group", () => {
  const group: UiTsGroup = { tsIds: [{ key: 1, uniqueString: "A" }] };

  const result = TsGroupReducer(group, {
    type: "add_ts_members",
    payload: {
      tsIds: [
        { key: 1, uniqueString: "A" },
        { key: 2, uniqueString: "B" },
      ],
    },
  });

  expect(result.tsIds?.map((ts) => ts.key)).toEqual([1, 2]);
});

test("remove_ts_member drops only the named member", () => {
  const group: UiTsGroup = {
    tsIds: [
      { key: 1, uniqueString: "A" },
      { key: 2, uniqueString: "B" },
    ],
  };

  const result = TsGroupReducer(group, {
    type: "remove_ts_member",
    payload: { key: 1 },
  });

  expect(result.tsIds?.map((ts) => ts.key)).toEqual([2]);
});

test("add_subgroups routes each combine mode to its own API field", () => {
  let group: UiTsGroup = {};

  for (const combine of ["include", "exclude", "intersect"] as const) {
    group = TsGroupReducer(group, {
      type: "add_subgroups",
      payload: { combine, groups: [{ groupId: 7, groupName: "sub" }] },
    });
  }

  expect(group.includeGroups).toHaveLength(1);
  expect(group.excludeGroups).toHaveLength(1);
  expect(group.intersectGroups).toHaveLength(1);
  expect(combineField("exclude")).toEqual("excludeGroups");
});

test("remove_subgroup only touches the list for that combine mode", () => {
  const group: UiTsGroup = {
    includeGroups: [{ groupId: 7 }],
    excludeGroups: [{ groupId: 7 }],
  };

  const result = TsGroupReducer(group, {
    type: "remove_subgroup",
    payload: { combine: "include", groupId: 7 },
  });

  expect(result.includeGroups).toHaveLength(0);
  expect(result.excludeGroups).toHaveLength(1);
});

test("add_criterion stores a full location as a site record", () => {
  const result = TsGroupReducer(
    {},
    {
      type: "add_criterion",
      payload: { criterion: { kind: "site", site: { siteId: 2 } } },
    },
  );

  expect(result.groupSites).toEqual([{ siteId: 2 }]);
  expect(result.groupAttrs).toBeUndefined();
});

test("add_criterion stores a scoped part as a name=value attribute", () => {
  const result = TsGroupReducer(
    {},
    {
      type: "add_criterion",
      payload: { criterion: { kind: "attr", name: "SubLocation", value: "Gate*" } },
    },
  );

  expect(result.groupAttrs).toEqual(["SubLocation=Gate*"]);
});

test("add_criterion ignores a duplicate attribute", () => {
  const group: UiTsGroup = { groupAttrs: ["Interval=1Hour"] };

  const result = TsGroupReducer(group, {
    type: "add_criterion",
    payload: { criterion: { kind: "attr", name: "Interval", value: "1Hour" } },
  });

  expect(result.groupAttrs).toEqual(["Interval=1Hour"]);
});

test("remove_criterion removes from the collection the key belongs to", () => {
  const group: UiTsGroup = {
    groupSites: [{ siteId: 2 }],
    groupDataTypes: [{ id: 5, standard: "CWMS", code: "Stage" }],
    groupAttrs: ["Interval=1Hour", "Duration=0"],
  };

  const withoutAttr = TsGroupReducer(group, {
    type: "remove_criterion",
    payload: { key: attrCriterionKey("Interval", "1Hour") },
  });
  expect(withoutAttr.groupAttrs).toEqual(["Duration=0"]);
  expect(withoutAttr.groupSites).toHaveLength(1);

  const withoutSite = TsGroupReducer(group, {
    type: "remove_criterion",
    payload: { key: siteCriterionKey(2) },
  });
  expect(withoutSite.groupSites).toHaveLength(0);
  expect(withoutSite.groupDataTypes).toHaveLength(1);
});

test("criteria rows round-trip the keys the reducer removes by", () => {
  const group: UiTsGroup = {
    groupSites: [{ siteId: 2 }],
    groupDataTypes: [{ id: 5, standard: "CWMS", code: "Stage" }],
    groupAttrs: ["Interval=1Hour"],
  };

  let result: UiTsGroup = group;
  for (const row of criteriaRows(group, siteLabel)) {
    result = TsGroupReducer(result, {
      type: "remove_criterion",
      payload: { key: row.key },
    });
  }

  expect(result.groupSites).toHaveLength(0);
  expect(result.groupDataTypes).toHaveLength(0);
  expect(result.groupAttrs).toHaveLength(0);
});
