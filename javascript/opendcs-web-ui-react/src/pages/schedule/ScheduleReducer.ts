import type { ApiScheduleEntry } from "opendcs-api";

export type UiSchedule = Partial<ApiScheduleEntry>;

export type ScheduleAction = { type: "save"; payload: UiSchedule };

export function ScheduleReducer(
  current: UiSchedule,
  action: ScheduleAction,
): UiSchedule {
  if (action.type === "save") {
    return { ...current, ...action.payload };
  }
  return current;
}
