import type { Meta, StoryObj } from "@storybook/react-vite";

import { SiteNameTypeSelect } from "./SiteNameTypeSelect";
import { WithRefLists } from "../../../.storybook/mock/WithRefLists";

const meta = {
  component: SiteNameTypeSelect,
  decorators: [WithRefLists],
} satisfies Meta<typeof SiteNameTypeSelect>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    current: undefined,
  },
  play: async ({ mount }) => {
    await mount();
  },
};

export const OtherSelected: Story = {
  args: {
    current: "local",
  },
  play: async ({ mount }) => {
    await mount();
  },
};
