import type { UiState } from "./Actions";

export type RowState<T extends string | number> = {
  [k in T]: UiState;
};

export const findResponsiveChildRowNode = (
  rowNode: HTMLTableRowElement,
): HTMLElement | null => {
  const sibling = rowNode.nextElementSibling as HTMLElement | null;
  if (!sibling || !sibling.classList.contains("child")) return null;
  return sibling;
};

export const queryDataTableRowNode = <T extends Element>(
  rowNode: HTMLTableRowElement,
  selector: string,
): T | null =>
  (rowNode.querySelector(selector) as T | null) ??
  (findResponsiveChildRowNode(rowNode)?.querySelector(selector) as T | null) ??
  null;
