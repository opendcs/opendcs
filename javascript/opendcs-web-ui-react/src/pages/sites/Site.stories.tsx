import type { Meta, StoryObj } from "@storybook/react-vite";

import Site from "./Site";
import { expect, fn, Mock } from "storybook/test";
import { ApiSite } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";

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
  play: async ({ mount }) => {
    await mount();
  },
};

export const WithSiteAsPromise: Story = {
  args: {
    site: Promise.resolve(site1),
  },
};

export const WithSiteInEdit: Story = {
  args: {
    site: site1,
    edit: true,
    actions: {
      save: fn((item: ApiSite) => {
        console.log(item);
      }),
      cancel: fn(),
    },
  },
  play: async ({ canvas, mount, args, parameters, userEvent }) => {
    const { i18n } = parameters;
    await mount();
    const mockSave = args.actions!.save! as Mock;
    mockSave.mockReset();

    const saveButton = await canvas.findByRole("button", {
      name: i18n.t("sites:save_site", { id: site1.siteId }),
    });
    const elevationInput = await canvas.findByLabelText(i18n.t("elevation"));
    const elevationUnitSelect = await canvas.findByLabelText(i18n.t("elevation_units"));
    await userEvent.type(elevationInput, "5");
    await userEvent.selectOptions(elevationUnitSelect, "ft");
    await userEvent.click(saveButton);

    await mount();
    expect(args.actions?.save).toHaveBeenCalled();

    console.log(mockSave.mock.calls);
    const newSite = mockSave.mock.calls[0][0];
    console.log(newSite);
    expect(newSite.elevation).toEqual("5");
    expect(newSite.elevUnits).toEqual("ft");

    const cancelButton = await canvas.findByRole("button", {
      name: i18n.t("sites:cancel_for", { id: site1.siteId }),
    });
    await userEvent.click(cancelButton);
    expect(args.actions?.cancel).toHaveBeenCalled();
  },
};
