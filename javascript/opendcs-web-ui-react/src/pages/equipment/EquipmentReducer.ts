import type { UiEquipmentModel } from "./Equipment";

export type EquipmentAction =
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } }
  | { type: "save"; payload: UiEquipmentModel };

export function EquipmentReducer(
  current: UiEquipmentModel,
  action: EquipmentAction,
): UiEquipmentModel {
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
