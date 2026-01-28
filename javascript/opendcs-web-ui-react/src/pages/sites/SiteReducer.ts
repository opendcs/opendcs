import type { UiSite } from "./Site";

export type SiteAction =
  // add/edit also the same
  | { type: "add_name"; payload: { type: string; name: string } }
  | { type: "change_type"; payload: { old_type: string; new_type: string } }
  | { type: "delete_name"; payload: { type: string } }
  // save/edit/add prop functionally equivalent
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } }
  | { type: "save"; payload: UiSite };

export function SiteReducer(currentSite: UiSite, action: SiteAction): UiSite {
  switch (action.type) {
    case "add_name": {
      return {
        ...currentSite,
        sitenames: {
          ...currentSite.sitenames,
          [action.payload.type]: action.payload.name,
        },
      };
    }
    case "change_type": {
      type sitenamesTypes = { [k: string]: string };
      const curKey: keyof sitenamesTypes = action.payload.old_type;
      const curValue = currentSite.sitenames![action.payload.old_type];
      const { [curKey]: tmp, ...currentSiteNames } = currentSite.sitenames!;

      return {
        ...currentSite,
        sitenames: {
          ...currentSiteNames,
          [action.payload.new_type]: curValue,
        },
      };
    }
    case "delete_name": {
      const { [action.payload.type]: _, ...names } = currentSite.sitenames!;
      return {
        ...currentSite,
        sitenames: {
          ...names,
        },
      };
    }
    case "save_prop": {
      return {
        ...currentSite,
        properties: {
          ...currentSite.properties,
          [action.payload.name]: action.payload.value,
        },
      };
    }
    case "delete_prop": {
      const { [action.payload.name]: _, ...props } = currentSite.properties!;
      return {
        ...currentSite,
        properties: {
          ...props,
        },
      };
    }
    case "save": {
      return {
        ...currentSite,
        ...action.payload,
      };
    }
  }
}
