import type { ApiPresentationElement, ApiPresentationGroup } from "opendcs-api";

export type UiPresentation = Partial<ApiPresentationGroup>;

// Elements lack a stable id from the API, so we key them by
// `${dataTypeStd}|${dataTypeCode}`. Both fields are required for a meaningful
// element, so the composite key is unique within a group.
export const elementKey = (
  e: Pick<ApiPresentationElement, "dataTypeStd" | "dataTypeCode">,
) => `${e.dataTypeStd ?? ""}|${e.dataTypeCode ?? ""}`;

export type PresentationAction =
  | { type: "save"; payload: UiPresentation }
  | {
      type: "save_element";
      payload: { originalKey?: string; element: ApiPresentationElement };
    }
  | { type: "delete_element"; payload: { key: string } };

export function PresentationReducer(
  current: UiPresentation,
  action: PresentationAction,
): UiPresentation {
  switch (action.type) {
    case "save":
      return { ...current, ...action.payload };
    case "save_element": {
      const elements = [...(current.elements ?? [])];
      const targetKey =
        action.payload.originalKey ?? elementKey(action.payload.element);
      const idx = elements.findIndex((e) => elementKey(e) === targetKey);
      if (idx >= 0) {
        elements[idx] = action.payload.element;
      } else {
        elements.push(action.payload.element);
      }
      return { ...current, elements };
    }
    case "delete_element": {
      const elements = (current.elements ?? []).filter(
        (e) => elementKey(e) !== action.payload.key,
      );
      return { ...current, elements };
    }
  }
}
