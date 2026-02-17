import { createContext, useContext } from "react";
import type { ApiUnit, ApiUnitConverter } from "opendcs-api";

export interface UnitsContextType {
  units: { [k: number]: ApiUnit };
  conversions: { [k: number]: ApiUnitConverter };
  getConversion: (
    id?: number,
    from?: string,
    to?: string,
  ) => ApiUnitConverter | undefined;
  ready: boolean;
}

export const defaultValue: UnitsContextType = {
  units: {},
  conversions: [],
  getConversion: () => {
    return undefined;
  },
  ready: false,
};

export const UnitsContext = createContext<UnitsContextType | undefined>(undefined);

export const useUnits = () => {
  const context = useContext(UnitsContext);
  if (context == undefined) {
    throw new Error("RefList isn't defined?");
  }
  return context;
};

export default UnitsContext;
