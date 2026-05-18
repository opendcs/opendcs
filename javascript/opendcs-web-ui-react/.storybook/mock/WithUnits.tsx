/**
 * Seeds the TanStack QueryClient with stub unit / conversion data so any
 * story that renders a component reading useUnitListQuery /
 * useUnitConversionsQuery sees ready data without a network round-trip.
 *
 * Assumes WithQueryClient is mounted higher in the decorator chain.
 */

import { Decorator } from "@storybook/react-vite";
import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo } from "react";
import { ApiUnit, ApiUnitConverter } from "opendcs-api";
import { unitKeys } from "../../src/queries/keys";

export const WithUnits: Decorator = (Story) => {
  const queryClient = useQueryClient();
  const units = useMemo<Record<number, ApiUnit>>(
    () => ({
      1: { name: "feet", abbr: "ft", family: "English", measures: "length" },
      2: { name: "meters", abbr: "m", family: "Metrics", measures: "length" },
      3: { name: "volts", abbr: "v", family: "Metrics", meaures: "voltage" },
      4: {
        name: "cubic feet per second",
        abbr: "cfs",
        family: "English",
        measures: "flow",
      },
      5: {
        name: "cubic meters per second",
        abbr: "cms",
        family: "Metrics",
        measures: "flow",
      },
      6: {
        name: "Raw undefined units",
        abbr: "raw",
        family: "univ",
        measures: "undefined",
      },
    }),
    [],
  );

  const conversions = useMemo<ApiUnitConverter[]>(
    () => [
      {
        ucId: 1,
        fromAbbr: "ft",
        toAbbr: "m",
        algorithm: "linear",
        a: 1.609344,
        b: 0.0,
      },
      { ucId: 2, fromAbbr: "cfs", toAbbr: "cms", algorithm: "linear" },
    ],
    [],
  );

  // ApiContext default in stories has org === "" (no localStorage org).
  useEffect(() => {
    const org = "";
    queryClient.setQueryData(unitKeys.list(org), units);
    queryClient.setQueryData(unitKeys.conversions(org), conversions);
  }, [queryClient, units, conversions]);

  return <Story />;
};
