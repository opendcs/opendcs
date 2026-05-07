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
    updatePassword: fn(),
  },
};

export const CanChange: Story = {
  args: { ...Default.args },
  play: async ({ mount, userEvent, args }) => {
    const canvas = await mount();
    const mockUpdatePassword = args!.updatePassword;

    const currentPassword = await canvas.findByLabelText("Current Password");
    const newPassword = await canvas.findByLabelText("New Password");
    const repeatPassword = await canvas.findByLabelText("Repeat Password");

    await userEvent.clear(currentPassword);
    await userEvent.clear(newPassword);
    await userEvent.clear(repeatPassword);

    const currentPasswordValue = "current password";
    const newPasswordValue = "new password";

    await userEvent.type(currentPassword, currentPasswordValue);
    await userEvent.type(newPassword, newPasswordValue);
    await userEvent.type(repeatPassword, newPasswordValue);

    const submitChange = await canvas.findByRole("button", { name: "Update Password" });
    await waitFor(() => expect(submitChange).not.toBeDisabled());
    await userEvent.click(submitChange);

    expect(mockUpdatePassword).toHaveBeenCalledWith(
      currentPasswordValue,
      newPasswordValue,
    );
  },
};
