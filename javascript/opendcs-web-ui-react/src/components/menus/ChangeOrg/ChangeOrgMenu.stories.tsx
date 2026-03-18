import type { Meta, StoryObj } from "@storybook/react-vite";
import { ChangeOrgMenu } from "./ChangeOrgMenu";
import { expect, fn, waitFor } from "storybook/test";
import { ApiOrganization } from "opendcs-api";
import { MOCK_ORGANIZATIONS } from "../../../../.storybook/mock/WithOrganization";

const meta = {
  component: ChangeOrgMenu,
} satisfies Meta<typeof ChangeOrgMenu>;

export default meta;

type Story = StoryObj<typeof meta>;

export const CanClickChange: Story = {
  args: {
    org: { name: "SPK" } as ApiOrganization,
    orgs: MOCK_ORGANIZATIONS,
    changeOrg: fn(),
  },
  play: async ({ args, mount, userEvent }) => {
    const canvas = await mount();

    const toggle = await canvas.findByRole("button", { name: "organization-settings" });
    await userEvent.click(toggle);

    const hq = await canvas.findByText("HQ");
    await userEvent.click(hq);

    await waitFor(() => {
      expect(args.changeOrg).toHaveBeenCalled();
    });
  },
};
