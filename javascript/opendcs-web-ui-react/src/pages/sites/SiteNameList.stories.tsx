import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, fn } from "storybook/test";
import { SiteNameList } from "./SiteNameList";
import { WithRefLists } from "../../../.storybook/mock/WithRefLists";

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
      { type: "CWMS", name: "Alder Springs", ui_state: "edit" },
      { type: "NWSHB5", name: "ALS" },
      { ui_state: "new" },
    ],
    actions: {
      add: fn(),
      edit: fn(),
      save: fn(),
      remove: fn(),
    },
  },
  play: async ({ canvas, mount, userEvent, parameters, args }) => {
    const { i18n } = parameters;
    await mount();

    const saveButton = await canvas.findByRole("button", {
      name: i18n.t("sites:site_names.save_for", {
        type: "CWMS",
        name: "Alder Springs",
      }),
    });
    await userEvent.click(saveButton);
    expect(args.actions?.save).toHaveBeenCalled();
  },
};
