import type { ApiNetList, ApiNetListItem } from "opendcs-api";

export type UiNetlist = Partial<ApiNetList>;

export type NetlistAction =
  | { type: "save"; payload: UiNetlist }
  // Items are keyed in the API map by uppercase transportId. Adds/updates and
  // removals key off transportId so the map stays consistent.
  | { type: "save_item"; payload: { item: ApiNetListItem } }
  | { type: "delete_item"; payload: { transportId: string } };

const itemKey = (transportId: string) => transportId.toUpperCase();

export function NetlistReducer(current: UiNetlist, action: NetlistAction): UiNetlist {
  switch (action.type) {
    case "save":
      return { ...current, ...action.payload };
    case "save_item": {
      const id = action.payload.item.transportId ?? "";
      if (!id) return current;
      return {
        ...current,
        items: {
          ...current.items,
          [itemKey(id)]: action.payload.item,
        },
      };
    }
    case "delete_item": {
      const items = { ...current.items };
      delete items[itemKey(action.payload.transportId)];
      return { ...current, items };
    }
  }
}
