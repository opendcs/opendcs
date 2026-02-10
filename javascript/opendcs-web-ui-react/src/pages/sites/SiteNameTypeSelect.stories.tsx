import type { Meta, StoryObj } from "@storybook/react-vite";

import { SiteNameTypeSelect } from "./SiteNameTypeSelect";
import { WithRefLists } from "../../../.storybook/mock/WithRefLists";
import { expect } from "storybook/test";

const meta = {
  component: SiteNameTypeSelect,
  decorators: [WithRefLists],
} satisfies Meta<typeof SiteNameTypeSelect>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    defaultValue: undefined,
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const selectBox = await canvas.findByRole("combobox", {
      name: i18n.t("sites:site_names.select"),
    });
    expect(selectBox).toHaveValue("cwms");
  },
};

export const OtherSelected: Story = {
  args: {
    defaultValue: "local",
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const selectBox = await canvas.findByRole("combobox", {
      name: i18n.t("sites:site_names.select"),
    });
    expect(selectBox).toHaveValue("local");
  },
};

export const ExistingNames: Story = {
  args: {
    defaultValue: "local",
    existing: [{ type: "cwms" }],
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const selectBox = await canvas.findByRole("combobox", {
      name: i18n.t("sites:site_names.select"),
    });
    expect(selectBox).toHaveValue("local");
  },
};
