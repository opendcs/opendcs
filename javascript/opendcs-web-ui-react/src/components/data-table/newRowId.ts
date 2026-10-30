const NEW_ROW_ID_PREFIX = "__appdt_new_";

/** The synthetic id AppDataTable gives the `n`th row added through `inlineEdit.newTemplate`. */
export function newRowId(n: number): string {
  return `${NEW_ROW_ID_PREFIX}${n}`;
}

/** Whether `rowId` belongs to an inline-edit row that has not been saved yet. */
export function isNewRowId(rowId: string): boolean {
  return rowId.startsWith(NEW_ROW_ID_PREFIX);
}

/**
 * `rowId` as it should read in a row's labels. A new row has no name of its own
 * yet and its synthetic id means nothing to a screen reader, so `newRowLabel`
 * turns its counter into a friendly name, e.g. "new property 1".
 */
export function rowLabel(rowId: string, newRowLabel: (n: number) => string): string {
  return isNewRowId(rowId)
    ? newRowLabel(Number(rowId.slice(NEW_ROW_ID_PREFIX.length)))
    : rowId;
}
