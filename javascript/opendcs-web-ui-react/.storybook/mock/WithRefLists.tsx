/**
 * Decorator to provide appropriate enum values to components
 */

import { Decorator } from "@storybook/react-vite";
import {REFLIST_SITE_NAME_TYPE, RefListContext} from "../../src/contexts/data/RefListContext";
import { ApiRefList } from "../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useCallback } from "react";

export const WithRefLists: Decorator = (Story) => {
  const list = useCallback((k: string): ApiRefList => {
    // note: future work should just pull this all from he xml or something.
    if (k === REFLIST_SITE_NAME_TYPE) {
      return {
        enumName: k,
        items: {
          CWMS: { value: "CWMS" },
          local: { value: "local"},
          NWSHB5: { value: "NWSHB5"},
        }
      }
    } else {
      return {
        enumName: k,
        items: {}
      }
    }
    ;
  },[]);

  return (
    <RefListContext value={{refList: list}}>
      <Story />
    </RefListContext>
  );
}