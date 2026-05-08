import { expect, fn, waitFor } from "storybook/test";
import type { Meta, StoryObj } from "@storybook/react-vite";

import { PasswordChange } from "./PasswordChange";
import { BasicUser } from "../../../../.storybook/mock/TestUsers";

const meta = {
  component: PasswordChange,
} satisfies Meta<typeof PasswordChange>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    user: BasicUser,
    updatePassword: fn(
      async (currentPassword: string, newPassword: string): Promise<boolean> => {
        if (currentPassword !== "current password") {
          return false;
        } else if (newPassword === "Bad New Password") {
          return false;
        } else {
          return true;
        }
      },
    ),
  },
};

export const CanChange: Story = {
  args: { ...Default.args },
  play: async ({ mount, userEvent, args, parameters }) => {
    const canvas = await mount();
    const mockUpdatePassword = args!.updatePassword;
    const { i18n } = parameters;

    const currentPassword = await canvas.findByLabelText(
      i18n.t("user-data:password_change.current_password"),
    );
    const newPassword = await canvas.findByLabelText(
      i18n.t("user-data:password_change.new_password"),
    );
    const repeatPassword = await canvas.findByLabelText(
      i18n.t("user-data:password_change.repeat_password"),
    );

    await userEvent.clear(currentPassword);
    await userEvent.clear(newPassword);
    await userEvent.clear(repeatPassword);

    const currentPasswordValue = "current password";
    const newPasswordValue = "new password";

    await userEvent.type(currentPassword, currentPasswordValue);
    await userEvent.type(newPassword, newPasswordValue);
    await userEvent.type(repeatPassword, newPasswordValue);

    const submitChange = await canvas.findByRole("button", {
      name: i18n.t("user-data:password_change.update_password"),
    });
    await waitFor(() => expect(submitChange).not.toBeDisabled());
    await userEvent.click(submitChange);

    expect(mockUpdatePassword).toHaveBeenCalledWith(
      currentPasswordValue,
      newPasswordValue,
    );
  },
};
