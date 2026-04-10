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
  play: async ({ args, mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const toggle = await canvas.findByRole("button", {
      name: i18n.t("Change Organization"),
    });
    await userEvent.click(toggle);

    const hq = await canvas.findByText("HQ");
    await userEvent.click(hq);

    await waitFor(() => {
      expect(args.changeOrg).toHaveBeenCalled();
    });
  },
};
