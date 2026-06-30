import type { Decorator, Meta, StoryObj } from "@storybook/react-vite";

import { UserMenu } from "./UserMenu";
import { expect, fn } from "storybook/test";
import { act } from "react";
import {
  ApiContext,
  defaultValue as apiDefault,
} from "../../../contexts/app/ApiContext";
import { BasicUser } from "../../../../.storybook/mock/TestUsers";
import { AuthContext } from "../../../contexts/app/AuthContext";

const meta = {
  component: UserMenu,
} satisfies Meta<typeof UserMenu>;

export default meta;

type Story = StoryObj<typeof meta>;

const authDecorator: Decorator = (Story) => (
  <ApiContext value={apiDefault}>
    <AuthContext
      value={{
        user: BasicUser,
        isLoading: false,
        loginSchemes: {},
        setSchemes: fn(),
        setUser: fn(),
        logout: fn(),
      }}
    >
      <Story />
    </AuthContext>
  </ApiContext>
);

export const Default: Story = {
  args: {
    logout: fn(),
  },
  decorators: [authDecorator],
  play: async ({ mount }) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const _canvas = await mount();
  },
};

export const CanClickLogout: Story = {
  args: {
    logout: fn(),
  },
  decorators: [authDecorator],
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
