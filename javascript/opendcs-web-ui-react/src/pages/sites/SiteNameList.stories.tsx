import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, fn } from "storybook/test";
import { SiteNameList } from "./SiteNameList";
import { WithRefLists } from "../../../.storybook/mock/WithRefLists";
import { act } from "@testing-library/react";

const meta = {
  component: SiteNameList,
  decorators: [WithRefLists],
} satisfies Meta<typeof SiteNameList>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    siteNames: [],
    actions: {
      add: () => {},
      edit: () => {},
      remove: () => {},
      save: () => {},
    },
  },
};

export const WithNames: Story = {
  args: {
    siteNames: [
      { type: "CWMS", name: "Alder Springs" },
      { type: "NWSHB5", name: "ALS" },
    ],
  },
};

export const WithNamesInEdit: Story = {
  args: {
    siteNames: [
      { type: "CWMS", name: "Alder Springs" },
      { type: "NWSHB5", name: "ALS" },
    ],
    actions: {
      add: fn(),
      save: fn(),
      remove: fn(),
    },
    edit: true,
  },
  play: async ({ mount, userEvent, parameters, args }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editButton = await canvas.findByRole("button", {
      name: i18n.t("sites:site_names.edit_for", {
        type: "CWMS",
        name: "Alder Springs",
      }),
    });
    await act(async () => userEvent.click(editButton));
    const cwmsValue = await canvas.findByRole("textbox", {
      name: i18n.t("sites:site_names.value_input_for", { type: "CWMS" }),
    });
    expect(cwmsValue).toBeInTheDocument();
  },
};
