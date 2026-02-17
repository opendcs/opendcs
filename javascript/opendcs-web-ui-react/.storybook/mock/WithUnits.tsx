/**
 * Decorator to provide appropriate enum values to components
 */

import { Decorator } from "@storybook/react-vite";

import { useCallback, useEffect, useMemo } from "react";
import { useState } from "storybook/internal/preview-api";
import { ApiUnit, ApiUnitConverter } from "opendcs-api";
import {
  defaultValue,
  UnitsContext,
  UnitsContextType,
} from "../../src/contexts/data/UnitsContext";

export const WithUnits: Decorator = (Story) => {
  const units = useMemo<Record<number, ApiUnit>>(() => {
    return {
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
    };
  }, []);

  const conversions = useMemo<ApiUnitConverter[]>(() => {
    return [
      {
        ucId: 1,
        fromAbbr: "ft",
        toAbbr: "m",
        algorithm: "linear",
        a: 1.609344,
        b: 0.0,
      },
      { ucId: 2, fromAbbr: "cfs", toAbbr: "cms", algorithm: "linear" },
    ];
  }, []);

  const context: UnitsContextType = useMemo(() => {
    return {
      ...defaultValue,
      units: units,
      conversions: conversions,
      ready: true,
    };
  }, [units, conversions]);

  return (
    <UnitsContext value={context}>
      <Story />
    </UnitsContext>
  );
};
