import type { ApiDataType, ApiSiteRef, ApiTsGroup } from "opendcs-api";

/**
 * "Other criteria" a group can filter time series by, mirroring the desktop
 * group editor's bottom panel ("Select by site, data-type, interval, or
 * statistics-code").
 *
 * Three of the TSID parts have a *full* form that the API stores as a resolved
 * database record rather than a name=value attribute:
 *   - full Location -> ApiTsGroup.groupSites
 *   - full Param    -> ApiTsGroup.groupDataTypes
 * Every other part (including the base/sub halves of location, param and
 * version) is stored in ApiTsGroup.groupAttrs as "Name=Value".
 *
 * See decodes.tsdb.TsGroupMemberType for the attribute names the toolkit
 * understands.
 */
/** Pseudo part names for the two criteria stored as records, not attributes. */
export const PART_LOCATION = "Location";
export const PART_PARAM = "Param";

export type CriterionKind = "site" | "datatype" | "attr";

export interface Criterion {
  /** Stable identity for table rows and removal; unique within a group. */
  key: string;
  kind: CriterionKind;
  /** TSID part as displayed, e.g. "Location", "BaseParam", "Interval". */
  part: string;
  /** Displayed value, e.g. a site name, "CWMS:Stage", or "1Hour". */
  value: string;
  /** Set for kind "site" — the referenced site record. */
  siteId?: number;
  /** Set for kind "datatype" — the referenced data type record. */
  dataTypeId?: number;
}

/**
 * Split a stored "Name=Value" attribute. Only the first `=` separates the two,
 * so a value that itself contains `=` survives the round trip.
 */
export const parseGroupAttr = (raw: string): { name: string; value: string } => {
  const idx = raw.indexOf("=");
  if (idx < 0) return { name: raw.trim(), value: "" };
  return { name: raw.slice(0, idx).trim(), value: raw.slice(idx + 1) };
};

export const formatGroupAttr = (name: string, value: string): string =>
  `${name}=${value}`;

export const attrCriterionKey = (name: string, value: string): string =>
  `attr:${formatGroupAttr(name, value)}`;

export const siteCriterionKey = (siteId: number | undefined): string =>
  `site:${siteId ?? ""}`;

export const dataTypeCriterionKey = (dataTypeId: number | undefined): string =>
  `datatype:${dataTypeId ?? ""}`;

/**
 * GET /tsgroup returns groupDataTypes carrying only `id`, so a caller that has
 * the data type catalog should resolve the id and pass the result as
 * `criteriaRows`' `typeLabel` rather than relying on this fallback.
 */
export const dataTypeLabel = (dt: ApiDataType): string =>
  dt.displayName || [dt.standard, dt.code].filter(Boolean).join(":") || String(dt.id);

/**
 * Flatten a group's site, data-type and attribute criteria into the single
 * "TSID Part / Value" list the desktop editor presents.
 *
 * @param siteLabel resolves a site reference to the name to display, so the
 *   caller controls the preferred site-name type.
 */
export const criteriaRows = (
  group: Pick<ApiTsGroup, "groupSites" | "groupDataTypes" | "groupAttrs">,
  siteLabel: (site: ApiSiteRef) => string,
  typeLabel: (dt: ApiDataType) => string = dataTypeLabel,
): Criterion[] => [
  ...(group.groupSites ?? []).map((site) => ({
    key: siteCriterionKey(site.siteId),
    kind: "site" as const,
    part: PART_LOCATION,
    value: siteLabel(site),
    siteId: site.siteId,
  })),
  ...(group.groupDataTypes ?? []).map((dt) => ({
    key: dataTypeCriterionKey(dt.id),
    kind: "datatype" as const,
    part: PART_PARAM,
    value: typeLabel(dt),
    dataTypeId: dt.id,
  })),
  ...(group.groupAttrs ?? []).map((raw) => {
    const { name, value } = parseGroupAttr(raw);
    return {
      key: attrCriterionKey(name, value),
      kind: "attr" as const,
      part: name,
      value,
    };
  }),
];

/**
 * Split a time series unique string into its TSID parts for display.
 *
 * CWMS identifiers are six dot-separated parts
 * (location.param.paramType.interval.duration.version); other databases use a
 * different shape, so anything that isn't six parts yields empty columns and
 * the caller falls back to showing the whole identifier.
 */
export interface TsidParts {
  location: string;
  param: string;
  paramType: string;
  interval: string;
  duration: string;
  version: string;
}

const EMPTY_PARTS: TsidParts = {
  location: "",
  param: "",
  paramType: "",
  interval: "",
  duration: "",
  version: "",
};

export const tsidParts = (uniqueString: string | undefined): TsidParts => {
  const parts = (uniqueString ?? "").split(".");
  if (parts.length !== 6) return EMPTY_PARTS;
  const [location, param, paramType, interval, duration, version] = parts;
  return { location, param, paramType, interval, duration, version };
};

/**
 * CWMS splits location, param and version identifiers into a base part before
 * the first hyphen and a sub part after it — "ABC-Spillway1-Gate1" has base
 * "ABC" and sub "Spillway1-Gate1".
 */
export const splitBaseSub = (value: string): { base: string; sub: string } => {
  const idx = value.indexOf("-");
  if (idx < 0) return { base: value, sub: "" };
  return { base: value.slice(0, idx), sub: value.slice(idx + 1) };
};

/**
 * Name to show for a site criterion: the user's preferred site-name type when
 * the site has one, otherwise any name it does have. Mirrors
 * decodes.db.Site#getPreferredName().
 */
export const siteCriterionLabel = (site: ApiSiteRef, preferredType: string): string => {
  const entries = Object.entries(site.sitenames ?? {});
  const match = preferredType
    ? entries.find(([type]) => type.toLowerCase() === preferredType.toLowerCase())
    : undefined;
  return match?.[1] ?? entries[0]?.[1] ?? site.publicName ?? String(site.siteId ?? "");
};
