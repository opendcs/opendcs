import type { UiPlatform } from "./Platform";

export type PlatformAction =
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } }
  | { type: "save"; payload: UiPlatform };

export function PlatformReducer(
  currentPlatform: UiPlatform,
  action: PlatformAction,
): UiPlatform {
  switch (action.type) {
    case "save_prop": {
      return {
        ...currentPlatform,
        properties: {
          ...currentPlatform.properties,
          [action.payload.name]: action.payload.value,
        },
      };
    }
    case "delete_prop": {
      const props = { ...currentPlatform.properties };
      delete props[action.payload.name];
      return {
        ...currentPlatform,
        properties: {
          ...props,
        },
      };
    }
    case "save": {
      return {
        ...currentPlatform,
        ...action.payload,
      };
    }
  }
}
