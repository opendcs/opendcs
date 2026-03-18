import type { Meta, StoryObj } from "@storybook/react-vite";

import { UserMenu } from "./UserMenu";
import { expect, fn } from "storybook/test";
import { act } from "react";

const meta = {
  component: UserMenu,
} satisfies Meta<typeof UserMenu>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    user: { email: "Test Email" },
    logout: fn(),
  },
  play: async ({ mount }) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const _canvas = await mount();
  },
};

export const CanClickLogout: Story = {
  args: {
    user: { email: "Test Email" },
    logout: fn(),
  },
  play: async ({ args, mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const toggle = await canvas.findByRole("button", { name: i18n.t("user-settings") });
    await act(async () => userEvent.click(toggle));

    const logout = await canvas.findByRole("button", { name: i18n.t("logout") });
    await act(async () => userEvent.click(logout));

    expect(args.logout).toHaveBeenCalled();
  },
};
