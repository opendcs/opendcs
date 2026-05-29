import type { ApiDataSource, ApiDataSourceGroupMember } from "opendcs-api";

export type UiDataSource = Partial<ApiDataSource>;

export type DataSourceAction =
  | { type: "save"; payload: UiDataSource }
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } }
  // Group members are other data sources attached by name (hot-backup /
  // round-robin groups). The chooser adds by name; removal is by name too.
  | { type: "add_members"; payload: { members: ApiDataSourceGroupMember[] } }
  | { type: "remove_member"; payload: { name: string } };

export function DataSourceReducer(
  current: UiDataSource,
  action: DataSourceAction,
): UiDataSource {
  switch (action.type) {
    case "save":
      return { ...current, ...action.payload };
    case "save_prop":
      return {
        ...current,
        props: {
          ...current.props,
          [action.payload.name]: action.payload.value,
        },
      };
    case "delete_prop": {
      const props = { ...current.props };
      delete props[action.payload.name];
      return { ...current, props };
    }
    case "add_members": {
      const existing = current.groupMembers ?? [];
      const seen = new Set(existing.map((m) => m.dataSourceName));
      const merged = [...existing];
      for (const member of action.payload.members) {
        if (!seen.has(member.dataSourceName)) {
          merged.push(member);
          seen.add(member.dataSourceName);
        }
      }
      return { ...current, groupMembers: merged };
    }
    case "remove_member":
      return {
        ...current,
        groupMembers: (current.groupMembers ?? []).filter(
          (m) => m.dataSourceName !== action.payload.name,
        ),
      };
  }
}
