import type { UiTransportMedium } from "./TransportMedium";

export type TransportMediumAction =
  | { type: "save"; payload: UiTransportMedium }
  | {
      type: "change_type";
      payload: {
        mediumType: string;
        /**
         * Category-specific fields previously seen for the *new* mediumType's
         * category (or undefined / empty if it has never been visited in this
         * edit session). Carried by the caller via a useRef snapshot so going
         * A → B → A restores A's original field values instead of leaving
         * them blank.
         */
        restore?: UiTransportMedium;
      };
    };

export function TransportMediumReducer(
  current: UiTransportMedium,
  action: TransportMediumAction,
): UiTransportMedium {
  if (action.type === "save") {
    return { ...current, ...action.payload };
  }
  if (action.type === "change_type") {
    // Start from the snapshot for the incoming category (so previously
    // entered category-specific fields come back), then overlay the user's
    // latest common-field edits and the new mediumType.
    return {
      ...(action.payload.restore ?? {}),
      mediumType: action.payload.mediumType,
      mediumId: current.mediumId,
      scriptName: current.scriptName,
      timezone: current.timezone,
      timeAdjustment: current.timeAdjustment,
    };
  }
  return current;
}
