import type { UiState } from "./Actions";

export type RowState<T extends string | number> = {
  [k in T]: UiState;
};
