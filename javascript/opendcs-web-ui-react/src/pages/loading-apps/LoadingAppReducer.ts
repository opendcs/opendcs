import type { UiLoadingApp } from "./LoadingApp";

export type LoadingAppAction =
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } }
  | { type: "save"; payload: UiLoadingApp };

export function LoadingAppReducer(
  current: UiLoadingApp,
  action: LoadingAppAction,
): UiLoadingApp {
  switch (action.type) {
    case "save_prop": {
      return {
        ...current,
        properties: {
          ...current.properties,
          [action.payload.name]: action.payload.value,
        },
      };
    }
    case "delete_prop": {
      const props = { ...current.properties };
      delete props[action.payload.name];
      return { ...current, properties: props };
    }
    case "save": {
      return { ...current, ...action.payload };
    }
  }
}
