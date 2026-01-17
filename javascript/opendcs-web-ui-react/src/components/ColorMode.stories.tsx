import type { Meta, StoryObj } from "@storybook/react-vite";

import { ColorModes } from "./ColorMode";
import { Nav } from "react-bootstrap";
import { ModeIcons } from "./ModeIcon";

const meta = {
  component: ColorModes,
  decorators: [
    (Story) => {
      return (
        <>
          <ModeIcons />

          <Nav>
            <Story />
          </Nav>
        </>
      );
    },
  ],
} satisfies Meta<typeof ColorModes>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};
