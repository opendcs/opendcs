import type { Meta, StoryObj } from "@storybook/react-vite";

import { DecodesHeaderTypeSelect } from "./HeaderTypeSelect";

const meta = {
  component: DecodesHeaderTypeSelect,
} satisfies Meta<typeof DecodesHeaderTypeSelect>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
  play: async ({ mount }) => {
    const _canvas = await mount();
  },
};
