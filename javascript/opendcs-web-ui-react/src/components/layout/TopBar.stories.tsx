import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, fn } from "storybook/test";
import { TopBar } from "./TopBar";
import { ModeIcons } from "../ModeIcon";
import { AuthContext } from "../../contexts/app/AuthContext";

const meta = {
  component: TopBar,
  decorators: [
    (Story) => {
      return (
        <>
          <ModeIcons />
          <Story />
        </>
      );
    },
  ],
} satisfies Meta<typeof TopBar>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  play: async ({ canvas, mount }) => {
    await mount();
    const text = await canvas.findByText("OpenDCS");
    expect(text).toBeInTheDocument();
  },
};

export const WithUser: Story = {
  args: {},
  decorators: [
    (Story) => {
      return (
        <AuthContext
          value={{
            logout: fn(),
            setUser: fn(),
            user: { email: "testuser@example.com" },
          }}
        >
          <Story />
        </AuthContext>
      );
    },
  ],
  play: async ({ canvas, mount }) => {
    await mount();
    const text = await canvas.findByText("OpenDCS");
    expect(text).toBeInTheDocument();
  },
};
