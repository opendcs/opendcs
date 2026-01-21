import type { Meta, StoryObj } from "@storybook/react-vite";

import Site from "./Site";

const meta = {
  component: Site,
} satisfies Meta<typeof Site>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};


export const WithSite: Story = {
  args: {
    site: {
      siteId: 1,
      description: "A test"
    }
  }
}
