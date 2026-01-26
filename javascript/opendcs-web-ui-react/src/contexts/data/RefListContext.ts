import { createContext, useContext } from "react";
import type { ApiRefList } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";


export interface RefListContextType {
    refList: (k: string) => ApiRefList
};

export const defaultValue: RefListContextType = {
  refList: () => {
    return {};
  },
};


export const RefListContext = createContext<RefListContextType|undefined>(undefined);

export const useRefList = () => {
  const context = useContext(RefListContext);
  if (context == undefined) {
    throw new Error("RefList isn't defined?");
  }
  return context;
};

export default RefListContext;

// Constants for common ref list names.
export const REFLIST_SITE_NAME_TYPE = "SiteNameType";
