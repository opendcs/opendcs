import type { ApiComputation } from "opendcs-api";
import {
  isNow,
  parseAbsolute,
  parseNowMinus,
  formatAbsolute,
} from "../../../components/controls/idTime";

/** Convert the three API effective-time fields into the string format SinceUntilEditor uses. */
export const apiTimeToEditorValue = (
  type: string | undefined,
  date: Date | undefined | null,
  interval: string | undefined,
): string | undefined => {
  switch (type) {
    case "Now":
      return "now";
    case "Now -":
      return interval ? `now - ${interval}` : undefined;
    case "Calendar": {
      if (!date) return undefined;
      const d = date instanceof Date ? date : new Date(date as unknown as string);
      return Number.isNaN(d.getTime()) ? undefined : formatAbsolute(d);
    }
    default:
      return undefined; // "No Limit" or unset → empty → editor shows No Limit
  }
};

/** Parse SinceUntilEditor's emitted string back into the three API start fields. */
export const editorValueToStartFields = (
  value: string,
): Pick<
  ApiComputation,
  "effectiveStartType" | "effectiveStartDate" | "effectiveStartInterval"
> => {
  if (!value)
    return {
      effectiveStartType: "No Limit",
      effectiveStartDate: undefined,
      effectiveStartInterval: undefined,
    };
  if (isNow(value))
    return {
      effectiveStartType: "Now",
      effectiveStartDate: undefined,
      effectiveStartInterval: undefined,
    };
  const amount = parseNowMinus(value);
  if (amount !== undefined)
    return {
      effectiveStartType: "Now -",
      effectiveStartInterval: amount,
      effectiveStartDate: undefined,
    };
  const abs = parseAbsolute(value);
  if (abs)
    return {
      effectiveStartType: "Calendar",
      effectiveStartDate: abs,
      effectiveStartInterval: undefined,
    };
  return {
    effectiveStartType: "No Limit",
    effectiveStartDate: undefined,
    effectiveStartInterval: undefined,
  };
};

/** Parse SinceUntilEditor's emitted string back into the three API end fields. */
export const editorValueToEndFields = (
  value: string,
): Pick<
  ApiComputation,
  "effectiveEndType" | "effectiveEndDate" | "effectiveEndInterval"
> => {
  if (!value)
    return {
      effectiveEndType: "No Limit",
      effectiveEndDate: undefined,
      effectiveEndInterval: undefined,
    };
  if (isNow(value))
    return {
      effectiveEndType: "Now",
      effectiveEndDate: undefined,
      effectiveEndInterval: undefined,
    };
  const amount = parseNowMinus(value);
  if (amount !== undefined)
    return {
      effectiveEndType: "Now -",
      effectiveEndInterval: amount,
      effectiveEndDate: undefined,
    };
  const abs = parseAbsolute(value);
  if (abs)
    return {
      effectiveEndType: "Calendar",
      effectiveEndDate: abs,
      effectiveEndInterval: undefined,
    };
  return {
    effectiveEndType: "No Limit",
    effectiveEndDate: undefined,
    effectiveEndInterval: undefined,
  };
};
