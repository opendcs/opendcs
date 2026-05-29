import type { ApiRouting } from "opendcs-api";

export type UiRouting = Partial<ApiRouting>;

export type RoutingAction =
  | { type: "save"; payload: UiRouting }
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } }
  // Platforms attach either by name (sc:DCP_NAME -> platformNames) or by DCP
  // address (sc:DCP_ADDRESS -> platformIds). The chooser adds by name; both
  // kinds can be removed.
  | { type: "add_platform_names"; payload: { names: string[] } }
  | { type: "remove_platform_name"; payload: { name: string } }
  | { type: "remove_platform_id"; payload: { id: string } }
  | { type: "add_netlists"; payload: { netlistNames: string[] } }
  | { type: "remove_netlist"; payload: { netlistName: string } };

export function RoutingReducer(current: UiRouting, action: RoutingAction): UiRouting {
  switch (action.type) {
    case "save":
      return { ...current, ...action.payload };
    case "save_prop":
      return {
        ...current,
        properties: {
          ...current.properties,
          [action.payload.name]: action.payload.value,
        },
      };
    case "delete_prop": {
      const props = { ...current.properties };
      delete props[action.payload.name];
      return { ...current, properties: props };
    }
    case "add_platform_names": {
      const existing = current.platformNames ?? [];
      const seen = new Set(existing);
      const merged = [...existing];
      for (const name of action.payload.names) {
        if (!seen.has(name)) {
          merged.push(name);
          seen.add(name);
        }
      }
      return { ...current, platformNames: merged };
    }
    case "remove_platform_name":
      return {
        ...current,
        platformNames: (current.platformNames ?? []).filter(
          (name) => name !== action.payload.name,
        ),
      };
    case "remove_platform_id":
      return {
        ...current,
        platformIds: (current.platformIds ?? []).filter(
          (id) => id !== action.payload.id,
        ),
      };
    case "add_netlists": {
      const existing = current.netlistNames ?? [];
      const seen = new Set(existing);
      const merged = [...existing];
      for (const name of action.payload.netlistNames) {
        if (!seen.has(name)) {
          merged.push(name);
          seen.add(name);
        }
      }
      return { ...current, netlistNames: merged };
    }
    case "remove_netlist":
      return {
        ...current,
        netlistNames: (current.netlistNames ?? []).filter(
          (name) => name !== action.payload.netlistName,
        ),
      };
  }
}
