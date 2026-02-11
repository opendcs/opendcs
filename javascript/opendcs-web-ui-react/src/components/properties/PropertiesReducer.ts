import type { Property } from "./Properties";

export type PropertiesAction =
  // save/edit also the same
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } };

export function PropertiesReducer(
  currentProperties: Property[],
  action: PropertiesAction,
): Property[] {
  switch (action.type) {
    case "save_prop": {
      const existing = currentProperties.filter(
        (ep) => ep.name !== action.payload.name,
      );
      return [...existing, action.payload];
    }
    case "delete_prop": {
      return [...currentProperties.filter((ep) => ep.name !== action.payload.name)];
    }
  }
}
