import type { UiTransportMedium } from "./TransportMedium";

export type TransportMediumAction = {
  type: "save";
  payload: UiTransportMedium;
};

export function TransportMediumReducer(
  current: UiTransportMedium,
  action: TransportMediumAction,
): UiTransportMedium {
  if (action.type === "save") {
    return { ...current, ...action.payload };
  }
  return current;
}
