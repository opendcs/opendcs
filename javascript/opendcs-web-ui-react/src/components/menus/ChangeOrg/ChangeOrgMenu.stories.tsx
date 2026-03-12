import type { Meta, StoryObj } from "@storybook/react-vite";
import { ChangeOrgMenu } from "./ChangeOrgMenu";
import { expect, fn, waitFor } from "storybook/test";

const meta = {
  component: ChangeOrgMenu,
} satisfies Meta<typeof ChangeOrgMenu>;

export default meta;

type Story = StoryObj<typeof meta>;

export const CanClickChange: Story = {
  args: {
    org: "SPK",
    orgs: ["SPK", "HQ", "LRL"],
    onChange: fn(),
  },
  play: async ({ args, mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { org } = parameters;

    const toggle = await canvas.findByRole("button", { name: org });
    await userEvent.click(toggle);

    const hq = await canvas.findByText("HQ");
    await userEvent.click(hq);

    await waitFor(() => {
      expect(args.onChange).toHaveBeenCalled();
    });
  },
};
