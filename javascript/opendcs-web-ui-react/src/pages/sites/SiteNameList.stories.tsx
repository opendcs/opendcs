import type { Meta, StoryObj } from "@storybook/react-vite";
import { fn } from "storybook/test";
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
     siteNames: {},
     actions: {
      add: () => {},
      edit: () => {},
      remove: () => {},
      save: () => {}
     }
  },
};


export const WithNames: Story = {
  args: {
     siteNames: {
      CWMS: "Alder Springs",
      NWSHB5: "ALS",
    },
  },
}

export const WithNamesInEdit: Story = {
  args: {
     siteNames: {
      CWMS: "Alder Springs",
      NWSHB5: "ALS",
    },
    actions: {
      edit: fn()
    }
  },
}