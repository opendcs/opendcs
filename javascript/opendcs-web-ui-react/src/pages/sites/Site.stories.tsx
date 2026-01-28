import type { Meta, StoryObj } from "@storybook/react-vite";

import Site from "./Site";
import { expect } from "storybook/test";

const meta = {
  component: Site,
} satisfies Meta<typeof Site>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    site: {},
  },
  play: async ({ canvas, mount, parameters }) => {
    const { i18n } = parameters;
    await mount();
    expect(await canvas.findByText(i18n.t("latitude"))).toBeInTheDocument();
  },
};

const site1 = {
  siteId: 1,
  description: "A test",
  properties: {
    prop1: "value1",
  },
  sitenames: {
    CWMS: "Alder Springs",
    NWSHB5: "ALS",
  },
};

export const WithSite: Story = {
  args: {
    site: site1,
  },
};

export const WithSiteAsPromise: Story = {
  args: {
    site: Promise.resolve(site1),
  },
};
