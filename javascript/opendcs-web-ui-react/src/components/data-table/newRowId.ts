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
 * `rowId` as it should read in a row's labels: a new row has no name of its own
 * yet, so it is labeled by its counter ("1", "2", ...).
 */
export function rowLabelId(rowId: string): string {
  return rowId.replace(NEW_ROW_ID_PREFIX, "");
}
