import type { Decorator, Meta, StoryObj } from "@storybook/react-vite";
import { expect, within } from "storybook/test";
import { useQueryClient } from "@tanstack/react-query";
import { useMemo } from "react";
import type { ApiUnit } from "opendcs-api";

import UnitSelect from "./UnitSelector";
import { unitKeys } from "../../queries/keys";

// Mirrors how the API actually returns units — code-point order, so every
// upper-case abbreviation sits ahead of every lower-case one ("Wh" before
// "ac-ft"), and "EU10" ahead of "EU9". The global WithUnits decorator seeds
// an all-lower-case list, which can't show either problem.
const API_ORDER: Record<number, ApiUnit> = {
  1: { name: "Watt hours", abbr: "Wh", family: "Metric", measures: "energy" },
  2: { name: "Centigrade", abbr: "C", family: "Metric", measures: "temperature" },
  3: { name: "example unit 10", abbr: "EU10", family: "univ", measures: "example" },
  4: { name: "example unit 9", abbr: "EU9", family: "univ", measures: "example" },
  5: { name: "acre-feet", abbr: "ac-ft", family: "English", measures: "volume" },
  6: {
    name: "cubic feet per second",
    abbr: "cfs",
    family: "English",
    measures: "flow",
  },
  7: { name: "Raw undefined units", abbr: "raw", family: "univ", measures: "raw" },
};

// Overrides the units the global WithUnits decorator already cached. Seeded
// during render, not in an effect, so the select finds the data on its first
// render rather than briefly falling back to the disabled placeholder.
const WithApiOrderUnits: Decorator = (Story) => {
  const queryClient = useQueryClient();
  useMemo(() => {
    queryClient.setQueryData(unitKeys.list(""), API_ORDER);
  }, [queryClient]);
  return <Story />;
};

const meta = {
  component: UnitSelect,
  decorators: [WithApiOrderUnits],
} satisfies Meta<typeof UnitSelect>;

export default meta;

type Story = StoryObj<typeof meta>;

const readOptions = (canvas: ReturnType<typeof within>): string[] =>
  within(canvas.getByRole("combobox"))
    .getAllByRole("option")
    .map((o) => o.textContent ?? "");

/**
 * Units are listed alphabetically rather than in the database order the API
 * returns them in. Case-insensitive, so lower-case abbreviations interleave
 * with upper-case ones instead of being grouped after them.
 */
export const UnitsAreSortedAlphabetically: Story = {
  args: { current: "cfs" },
  play: async ({ mount }) => {
    const canvas = await mount();
    await expect(readOptions(canvas)).toEqual([
      "ac-ft",
      "C",
      "cfs",
      "EU9",
      "EU10",
      "raw",
      "Wh",
    ]);
  },
};

/**
 * The collator is digit-aware, so "EU9" precedes "EU10" instead of sorting
 * character by character. Asserted on its own because a plain
 * case-insensitive sort would still pass the ordering story above for every
 * pair except this one.
 */
export const EmbeddedNumbersSortNumerically: Story = {
  args: { current: "EU9" },
  play: async ({ mount }) => {
    const canvas = await mount();
    const options = readOptions(canvas);
    await expect(options.indexOf("EU9")).toBeLessThan(options.indexOf("EU10"));
  },
};
