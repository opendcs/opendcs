import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect } from "storybook/test";

import { DecodesHeaderTypeSelect } from "./HeaderTypeSelect";
import RefListContext from "../../../contexts/data/RefListContext";

const meta = {
  component: DecodesHeaderTypeSelect,
} satisfies Meta<typeof DecodesHeaderTypeSelect>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
  play: async ({ mount }) => {
    await mount();
  },
};

/**
 * While the reference lists are still in flight the control stays a dropdown --
 * disabled instead of replaced by a status line, which used to push the select
 * out of the row entirely.
 */
export const Loading: Story = {
  args: { defaultValue: "GOES" },
  decorators: [
    (Story) => (
      <RefListContext value={{ refList: () => ({}), ready: false, failed: false }}>
        <Story />
      </RefListContext>
    ),
  ],
  play: async ({ mount }) => {
    const canvas = await mount();
    const select = canvas.getByRole("combobox");
    await expect(select).toBeDisabled();
    // The value already on the script stays readable.
    await expect(select).toHaveValue("GOES");
  },
};

/**
 * The lists failed to load, so "loading" would never resolve. Say so instead.
 */
export const Unavailable: Story = {
  args: {},
  decorators: [
    (Story) => (
      <RefListContext value={{ refList: () => ({}), ready: false, failed: true }}>
        <Story />
      </RefListContext>
    ),
  ],
  play: async ({ mount }) => {
    const canvas = await mount();
    const select = canvas.getByRole("combobox");
    await expect(select).toBeDisabled();
    await expect(select).toHaveTextContent("Unavailable");
    await expect(select).toHaveAttribute("title", "Reference list data unavailable");
  },
};
