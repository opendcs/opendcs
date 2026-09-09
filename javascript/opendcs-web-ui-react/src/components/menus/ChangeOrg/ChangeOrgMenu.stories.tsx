import type { Meta, StoryObj } from "@storybook/react-vite";
import { ChangeOrgMenu } from "./ChangeOrgMenu";
import { expect, fn, waitFor } from "storybook/test";
import { ApiOrganization } from "opendcs-api";
import { MOCK_ORGANIZATIONS } from "../../../../.storybook/mock/WithOrganization";
import { CwmsUser } from "../../../../.storybook/mock/TestUsers";

const meta = {
  component: ChangeOrgMenu,
} satisfies Meta<typeof ChangeOrgMenu>;

export default meta;

type Story = StoryObj<typeof meta>;

export const CanClickChange: Story = {
  args: {
    org: { name: "SPK" } as ApiOrganization,
    orgs: MOCK_ORGANIZATIONS,
    user: CwmsUser,
    changeOrg: fn(),
  },
  play: async ({ args, mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const toggle = await canvas.findByRole("button", {
      name: i18n.t("Change Organization"),
    });
    await userEvent.click(toggle);

    const swt = await canvas.findByText("SWT");
    await userEvent.click(swt);

    await waitFor(() => {
      expect(args.changeOrg).toHaveBeenCalled();
    });
  },
};
