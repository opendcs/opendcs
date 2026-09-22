import type { Meta, StoryObj } from "@storybook/react-vite";
import { ChangeOrgMenu } from "./ChangeOrgMenu";
import { expect, fn, waitFor } from "storybook/test";
import { ApiOrganization } from "opendcs-api";
import {
  MOCK_ORGANIZATIONS,
  MOCK_ORG_HIERARCHY,
} from "../../../../.storybook/mock/WithOrganization";
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

export const NestsOfficesUnderTheirParents: Story = {
  args: {
    org: { name: "SPK" } as ApiOrganization,
    orgs: MOCK_ORG_HIERARCHY,
    user: CwmsUser,
    changeOrg: fn(),
  },
  play: async ({ args, mount, userEvent, parameters, canvasElement }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await userEvent.click(
      await canvas.findByRole("button", { name: i18n.t("Change Organization") }),
    );

    // CwmsUser holds roles at SPK and SWT only. Their parent offices come
    // along so the nesting is visible, while MVD's branch has no role
    // anywhere beneath it and is dropped entirely.
    const menu = canvasElement.querySelector(".dropdown-menu")!;
    expect([...menu.children].map((item) => item.textContent)).toEqual([
      "HQ",
      "SPD",
      "SPK",
      "SWD",
      "SWT",
    ]);

    // An ancestor the user has no role in is a label, not a choice.
    await userEvent.click(await canvas.findByText("HQ"));
    expect(args.changeOrg).not.toHaveBeenCalled();

    await userEvent.click(await canvas.findByText("SWT"));
    await waitFor(() => {
      expect(args.changeOrg).toHaveBeenCalled();
    });
  },
};
