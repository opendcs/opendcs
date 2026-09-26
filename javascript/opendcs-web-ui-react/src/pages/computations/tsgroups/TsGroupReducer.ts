import type {
  ApiDataType,
  ApiSiteRef,
  ApiTimeSeriesIdentifier,
  ApiTsGroup,
  ApiTsGroupRef,
} from "opendcs-api";
import {
  attrCriterionKey,
  dataTypeCriterionKey,
  formatGroupAttr,
  parseGroupAttr,
  siteCriterionKey,
} from "./groupCriteria";

export type UiTsGroup = Partial<ApiTsGroup>;

/**
 * The three ways the desktop editor combines a sub-group into the group being
 * edited ("Add / Subtract / Intersect SubGroup"), and the ApiTsGroup field
 * each one lands in.
 */
export const SUBGROUP_COMBINES = ["include", "exclude", "intersect"] as const;
export type SubGroupCombine = (typeof SUBGROUP_COMBINES)[number];

const COMBINE_FIELDS: Record<
  SubGroupCombine,
  "includeGroups" | "excludeGroups" | "intersectGroups"
> = {
  include: "includeGroups",
  exclude: "excludeGroups",
  intersect: "intersectGroups",
};

export const combineField = (combine: SubGroupCombine) => COMBINE_FIELDS[combine];

/** A criterion as the user just picked it, before it is folded into the group. */
export type NewCriterion =
  | { kind: "site"; site: ApiSiteRef }
  | { kind: "datatype"; dataType: ApiDataType }
  | { kind: "attr"; name: string; value: string };

export type TsGroupAction =
  | { type: "save"; payload: UiTsGroup }
  | { type: "add_ts_members"; payload: { tsIds: ApiTimeSeriesIdentifier[] } }
  | { type: "remove_ts_member"; payload: { key: number } }
  | {
      type: "add_subgroups";
      payload: { combine: SubGroupCombine; groups: ApiTsGroupRef[] };
    }
  | {
      type: "remove_subgroup";
      payload: { combine: SubGroupCombine; groupId: number };
    }
  | { type: "add_criterion"; payload: { criterion: NewCriterion } }
  | { type: "remove_criterion"; payload: { key: string } };

const addUnique = <T>(existing: T[], added: T[], idOf: (item: T) => string): T[] => {
  const seen = new Set(existing.map(idOf));
  const merged = [...existing];
  for (const item of added) {
    const id = idOf(item);
    if (seen.has(id)) continue;
    seen.add(id);
    merged.push(item);
  }
  return merged;
};

const addCriterion = (current: UiTsGroup, criterion: NewCriterion): UiTsGroup => {
  switch (criterion.kind) {
    case "site":
      return {
        ...current,
        groupSites: addUnique(current.groupSites ?? [], [criterion.site], (s) =>
          siteCriterionKey(s.siteId),
        ),
      };
    case "datatype":
      return {
        ...current,
        groupDataTypes: addUnique(
          current.groupDataTypes ?? [],
          [criterion.dataType],
          (d) => dataTypeCriterionKey(d.id),
        ),
      };
    case "attr": {
      const attr = formatGroupAttr(criterion.name, criterion.value);
      return {
        ...current,
        groupAttrs: addUnique(current.groupAttrs ?? [], [attr], (a) => {
          const { name, value } = parseGroupAttr(a);
          return attrCriterionKey(name, value);
        }),
      };
    }
  }
};

const removeCriterion = (current: UiTsGroup, key: string): UiTsGroup => {
  if (key.startsWith("site:")) {
    return {
      ...current,
      groupSites: (current.groupSites ?? []).filter(
        (s) => siteCriterionKey(s.siteId) !== key,
      ),
    };
  }
  if (key.startsWith("datatype:")) {
    return {
      ...current,
      groupDataTypes: (current.groupDataTypes ?? []).filter(
        (d) => dataTypeCriterionKey(d.id) !== key,
      ),
    };
  }
  return {
    ...current,
    groupAttrs: (current.groupAttrs ?? []).filter((raw) => {
      const { name, value } = parseGroupAttr(raw);
      return attrCriterionKey(name, value) !== key;
    }),
  };
};

export function TsGroupReducer(current: UiTsGroup, action: TsGroupAction): UiTsGroup {
  switch (action.type) {
    case "save":
      return { ...current, ...action.payload };
    case "add_ts_members":
      return {
        ...current,
        tsIds: addUnique(current.tsIds ?? [], action.payload.tsIds, (ts) =>
          String(ts.key ?? ts.uniqueString),
        ),
      };
    case "remove_ts_member":
      return {
        ...current,
        tsIds: (current.tsIds ?? []).filter((ts) => ts.key !== action.payload.key),
      };
    case "add_subgroups": {
      const field = combineField(action.payload.combine);
      return {
        ...current,
        [field]: addUnique(current[field] ?? [], action.payload.groups, (g) =>
          String(g.groupId),
        ),
      };
    }
    case "remove_subgroup": {
      const field = combineField(action.payload.combine);
      return {
        ...current,
        [field]: (current[field] ?? []).filter(
          (g) => g.groupId !== action.payload.groupId,
        ),
      };
    }
    case "add_criterion":
      return addCriterion(current, action.payload.criterion);
    case "remove_criterion":
      return removeCriterion(current, action.payload.key);
  }
}
