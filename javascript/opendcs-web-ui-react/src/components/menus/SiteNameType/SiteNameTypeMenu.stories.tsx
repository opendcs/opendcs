import type { Meta, StoryObj } from "@storybook/react-vite";

import { SiteNameTypeMenu } from "./SiteNameTypeMenu";
import { Nav } from "react-bootstrap";

const meta = {
  component: SiteNameTypeMenu,
  decorators: [
    (Story) => (
      <Nav>
        <Story />
      </Nav>
    ),
  ],
} satisfies Meta<typeof SiteNameTypeMenu>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};
