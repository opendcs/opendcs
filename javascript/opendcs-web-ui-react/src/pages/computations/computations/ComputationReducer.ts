import type { UiComputation } from "./Computation";

export type ComputationAction =
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } }
  | { type: "save"; payload: UiComputation }
  | { type: "merge_algo_props"; payload: Record<string, string> };

export function ComputationReducer(
  current: UiComputation,
  action: ComputationAction,
): UiComputation {
  switch (action.type) {
    case "save_prop": {
      return {
        ...current,
        props: {
          ...current.props,
          [action.payload.name]: action.payload.value,
        },
      };
    }
    case "delete_prop": {
      const { [action.payload.name]: _, ...props } = current.props ?? {};
      return {
        ...current,
        props: {
          ...props,
        },
      };
    }
    case "save": {
      return {
        ...current,
        ...action.payload,
      };
    }
    case "merge_algo_props": {
      // Keep existing user values; add new keys from algorithm defaults
      const merged = { ...action.payload, ...(current.props ?? {}) };
      return { ...current, props: merged };
    }
  }
}
