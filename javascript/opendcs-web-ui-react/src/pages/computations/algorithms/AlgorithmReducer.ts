import type { UiAlgorithm } from "./Algorithm";

export type AlgorithmAction =
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } }
  | { type: "save"; payload: UiAlgorithm };

export function AlgorithmReducer(
  current: UiAlgorithm,
  action: AlgorithmAction,
): UiAlgorithm {
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
      const { [action.payload.name]: _, ...props } = current.props!;
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
  }
}
