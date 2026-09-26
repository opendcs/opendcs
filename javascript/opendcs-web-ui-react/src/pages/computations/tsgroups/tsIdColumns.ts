import type { ApiTimeSeriesIdentifier } from "opendcs-api";
import type { ColumnDef } from "../../../components/data-table";
import { tsidParts } from "./groupCriteria";

/**
 * Column set shared by the members table and its chooser: the identifier plus
 * the individual TSID parts the desktop editor breaks out. Parts render empty
 * for databases whose identifiers aren't six dot-separated fields, and the
 * full identifier column always carries the complete value.
 */
export const tsIdColumns = <T extends ApiTimeSeriesIdentifier>(
  t: (key: string) => string,
): ColumnDef<T>[] => [
  { data: "key", header: t("tsgroups:members.header.Key"), type: "num" },
  {
    data: "uniqueString",
    header: t("tsgroups:members.header.TimeSeries"),
    defaultContent: "",
  },
  {
    data: null,
    header: t("tsgroups:members.header.Location"),
    defaultContent: "",
    render: (_d, _type, row) => tsidParts(row.uniqueString).location,
  },
  {
    data: null,
    header: t("tsgroups:members.header.Param"),
    defaultContent: "",
    render: (_d, _type, row) => tsidParts(row.uniqueString).param,
  },
  {
    data: null,
    header: t("tsgroups:members.header.ParamType"),
    defaultContent: "",
    render: (_d, _type, row) => tsidParts(row.uniqueString).paramType,
  },
  {
    data: null,
    header: t("tsgroups:members.header.Interval"),
    defaultContent: "",
    render: (_d, _type, row) => tsidParts(row.uniqueString).interval,
  },
  {
    data: null,
    header: t("tsgroups:members.header.Duration"),
    defaultContent: "",
    render: (_d, _type, row) => tsidParts(row.uniqueString).duration,
  },
  {
    data: null,
    header: t("tsgroups:members.header.Version"),
    defaultContent: "",
    render: (_d, _type, row) => tsidParts(row.uniqueString).version,
  },
  {
    data: "description",
    header: t("tsgroups:members.header.Description"),
    defaultContent: "",
  },
];
