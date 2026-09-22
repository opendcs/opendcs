// Lists arrive from the API in database order, which renders as "everything
// upper case, then everything lower case" in a select. A code-point sort has
// the same flaw, so compare through a collator: case-insensitive, locale
// aware, and digit aware (so "EU10" follows "EU9").
const collator = new Intl.Collator(undefined, { sensitivity: "base", numeric: true });

export const compareStrings = (a?: string | null, b?: string | null): number =>
  collator.compare(a ?? "", b ?? "");
