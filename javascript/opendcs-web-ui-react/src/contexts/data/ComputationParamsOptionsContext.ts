import { createContext } from "react";

export interface ComputationParamsOptionsContextType {
  deltaTUnits: string[];
  ifMissingActions: string[];
  defaultIntervals: string[];
}

export const defaultValue: ComputationParamsOptionsContextType = {
  deltaTUnits: ["Seconds", "Minutes", "Hours", "Days", "Weeks", "Months", "Years"],
  ifMissingActions: ["FAIL", "IGNORE", "PREV", "NEXT", "INTERP", "CLOSEST"],
  defaultIntervals: [
    "0",
    "1Minute",
    "5Minutes",
    "10Minutes",
    "15Minutes",
    "30Minutes",
    "1Hour",
    "2Hours",
    "3Hours",
    "6Hours",
    "12Hours",
    "1Day",
    "1Week",
    "1Month",
    "1Year",
  ],
};

export const ComputationParamsOptionsContext =
  createContext<ComputationParamsOptionsContextType>(defaultValue);

export default ComputationParamsOptionsContext;
