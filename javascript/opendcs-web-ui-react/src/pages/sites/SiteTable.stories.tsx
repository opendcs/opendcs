import type { Decorator, Meta, StoryObj } from "@storybook/react-vite";

import { SitesTable, SiteTableProperties, TableSiteRef } from "./SitesTable";
import {
  ApiSite,
  ApiSiteRef,
} from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { expect, userEvent, waitFor } from "storybook/test";
import { useCallback, useState } from "react";

const meta = {
  component: SitesTable,
} satisfies Meta<typeof SitesTable>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    sites: [],
  },
  play: async ({ mount }) => {
    await mount();
  },
};

const WithSitesState: Decorator<SiteTableProperties> = (Story, context) => {
  const { args } = context;
  const [siteRefs, updateSiteRefs] = useState<TableSiteRef[]>(
    args.sites as TableSiteRef[],
  );

  const editSite = useCallback(
    (id: number) => {
      updateSiteRefs((prev) => {
        return prev.map((siteRef) => {
          if (siteRef.siteId === id) {
            return {
              ...siteRef,
              state: siteRef.state === "edit" ? undefined : "edit",
            };
          } else {
            return siteRef;
          }
        });
      });
    },
    [args],
  );

  return (
    <Story
      args={{
        ...args,
        sites: siteRefs,
        actions: { edit: editSite },
      }}
    />
  );
};

const sites: ApiSite[] = [
  {
    description: "Test site 1",
    sitenames: {
      CWMS: "TEST SITE 1",
    },
    siteId: 1,
  },
  {
    description: "Stargate command's backup location.",
    sitenames: {
      CWMS: "Alpha Site",
    },
    elevation: 5.21,
    elevUnits: "ft",
    publicName: "Clearly, this site is not public.",
    siteId: 2,
  },
  {
    siteId: 3,
    description: "A test",
    properties: {
      prop1: "value1",
    },
    sitenames: {
      CWMS: "Alder Springs",
      NWSHB5: "ALS",
    },
  },
];

const siteRefs: ApiSiteRef[] = sites.map((site) => {
  return {
    siteId: site.siteId,
    sitenames: site.sitenames,
    description: site.description,
    publicName: site.publicName,
  };
});

const getSite = async (id: number): Promise<ApiSite | undefined> => {
  return Promise.resolve(sites.find((site) => site.siteId == id));
};

export const WithSites: Story = {
  args: {
    sites: siteRefs,
    getSite: getSite,
  },
  decorators: [WithSitesState],
  play: async ({ mount }) => {
    await mount();
  },
};

export const WithExistingAddNewSite: Story = {
  args: {
    sites: siteRefs,
  },
  play: async ({ canvas, mount, parameters }) => {
    const { i18n } = parameters;
    await mount();
    const addSite = await canvas.findByRole("button", {
      name: i18n.t("sites:add_site"),
    });
    await userEvent.click(addSite);
    const negativeIndex = await waitFor(() => {
      return canvas.queryByText("-1");
    });
    expect(negativeIndex).toBeInTheDocument();
  },
};
