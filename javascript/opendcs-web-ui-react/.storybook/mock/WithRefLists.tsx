/**
 * Decorator to provide appropriate enum values to components
 */

import { Decorator } from "@storybook/react-vite";
import {RefListContext} from "../../src/contexts/RefListContext";
import { ApiRefList } from "../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";

export const WithRefLists: Decorator = (Story) => {

  const list = (k: string): ApiRefList => {
    // note: future work should just pull this all from he xml or something.
    if (k === "SiteNameType") {
      return {
        enumName: k,
        items: {
          CWMS: { value: "CWMS" },
          local: { value: "local"}
        }
      }
    } else {
      return {
        enumName: k,
        items: {}
      }
    }
    ;
  }

  return (
    <RefListContext value={{refList: list}}>
      <Story />
    </RefListContext>
  );
}