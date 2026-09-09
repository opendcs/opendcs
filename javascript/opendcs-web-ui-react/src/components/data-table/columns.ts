import type { ReactNode } from "react";
import type { ColumnDef } from "./AppDataTable";

/**
 * The numeric id column the list tables lead with: left aligned (DataTables
 * right aligns `num` columns by default) and showing "new" for a row that has
 * not been saved yet, since those carry a synthetic local id.
 */
export function idColumn<T>(data: keyof T & string, header: ReactNode): ColumnDef<T> {
  return {
    data,
    header,
    defaultContent: "new",
    className: "dt-left",
    type: "num",
  };
}

/**
 * A plain text column that renders blank when the row has no value for it.
 * `defaultContent` is what keeps DataTables from throwing its "Requested
 * unknown parameter" error on a row that omits the field.
 */
export function textColumn<T>(data: keyof T & string, header: ReactNode): ColumnDef<T> {
  return { data, header, defaultContent: "", type: "string" };
}

/**
 * Date column rendered in the browser's locale. DataTables gets the raw value
 * for sorting and filtering, so only the `"display"` pass is formatted, and an
 * unparseable or missing date shows as blank rather than "Invalid Date".
 */
export function dateColumn<T>(data: keyof T & string, header: ReactNode): ColumnDef<T> {
  return {
    data,
    header,
    defaultContent: "",
    type: "date",
    render: (value: unknown, type: string) => {
      if (type !== "display") return value;
      if (!value) return "";
      const d = value instanceof Date ? value : new Date(value as string);
      return Number.isNaN(d.getTime()) ? "" : d.toLocaleString();
    },
  };
}
