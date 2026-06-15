/**
 * Decorator to provide appropriate enum values to components
 */

import { Decorator } from "@storybook/react-vite";
import {
  REFLIST_DECODES_TRANSPORT_MEDIUM_TYPE,
  REFLIST_SITE_NAME_TYPE,
  REFLIST_UNIT_CONVERSION_ALGORITHM,
  RefListContext,
} from "../../src/contexts/data/RefListContext";
import { ApiRefList } from "../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useCallback, useMemo } from "react";

export const WithRefLists: Decorator = (Story) => {
  const refLists: { [k: string]: ApiRefList } = useMemo(() => {
    return {
      [REFLIST_SITE_NAME_TYPE]: {
        enumName: REFLIST_SITE_NAME_TYPE,
        defaultValue: "local",
        items: {
          cwms: { value: "cwms" },
          local: { value: "local" },
          nwshb5: { value: "nwshb5" },
        },
      },
      [REFLIST_DECODES_TRANSPORT_MEDIUM_TYPE]: {
        enumName: REFLIST_DECODES_TRANSPORT_MEDIUM_TYPE,
        items: {
          "goes-random": {
            value: "goes-random",
            description: "GOES DCP Random Message",
          },
          goes: { value: "goes", description: "GOES DCP" },
          "data-logger": {
            value: "data-logger",
            description: "Electronic Data Logger File",
          },
          iridium: { value: "iridium", description: "Iridium IMEI" },
          shef: { value: "shef", description: "Standard Hydromet Exchange Format" },
          other: { value: "other" },
        },
      },
      [REFLIST_UNIT_CONVERSION_ALGORITHM]: {
        enumName: REFLIST_UNIT_CONVERSION_ALGORITHM,
        items: {
          none: {
            value: "none",
            description: "no conversions",
            execClassName: "NoConversion",
          },
          linear: {
            value: "linear",
            description: "Y = Ax + B",
            execClassName: "LInearConverter",
          },
        },
      },
      DataConsumer: {
        enumName: "DataConsumer",
        items: {
          pipe: { value: "pipe" },
          file: { value: "file" },
          directory: { value: "directory" },
          cwms: { value: "cwms" },
          tsdb: { value: "tsdb" },
          socketclient: { value: "socketclient" },
        },
      },
      OutputFormat: {
        enumName: "OutputFormat",
        items: {
          "html-report": { value: "html-report" },
          "emit-ascii": { value: "emit-ascii" },
          shef: { value: "shef" },
          "human-readable": { value: "human-readable" },
        },
      },
      DataSourceType: {
        enumName: "DataSourceType",
        items: {
          lrgs: { value: "lrgs", description: "LRGS Network Server" },
          abstractweb: { value: "abstractweb", description: "Abstract Web Source" },
          file: { value: "file", description: "Single File" },
          directory: { value: "directory", description: "Directory of Files" },
          hotbackupgroup: {
            value: "hotbackupgroup",
            description: "Hot Backup Group",
          },
          roundrobingroup: {
            value: "roundrobingroup",
            description: "Round Robin Group",
          },
        },
      },
    };
  }, []);

  const list = useCallback(
    (k: string): ApiRefList => {
      // note: future work should just pull this all from the xml or something.
      console.log(refLists);
      return (
        refLists[k] || {
          enumName: k,
          items: {},
        }
      );
    },
    [refLists],
  );

  return (
    <RefListContext value={{ refList: list, ready: true }}>
      <Story />
    </RefListContext>
  );
};
