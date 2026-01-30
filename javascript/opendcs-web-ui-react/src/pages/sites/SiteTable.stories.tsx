import type { Decorator, Meta, StoryObj } from "@storybook/react-vite";

import { SitesTable, SiteTableProperties, TableSiteRef } from "./SitesTable";
import {
  ApiSite,
  ApiSiteRef,
} from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { expect, userEvent, waitFor } from "storybook/test";
import { use, useCallback, useMemo, useState } from "react";

const meta = {
  component: SitesTable,
} satisfies Meta<typeof SitesTable>;

export default meta;

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

const toSiteRefs = (sites: ApiSite[]): ApiSiteRef[] => {
  return sites.map((site) => {
    return {
      siteId: site.siteId,
      sitenames: site.sitenames,
      description: site.description,
      publicName: site.publicName,
    };
  });
};

const siteRefs: ApiSiteRef[] = toSiteRefs(sites);

const getSite = async (id: number): Promise<ApiSite | undefined> => {
  return Promise.resolve(sites.find((site) => site.siteId == id));
};

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
  const tmpSites: ApiSite[] = args.sites.map(
    (sf: ApiSiteRef) => use(getSite(sf.siteId!))!,
  );
  console.log(tmpSites);
  const [sites, updateSites] = useState<ApiSite[]>(tmpSites);
  const siteRefs = useMemo(() => toSiteRefs(sites), [sites]);

  const saveSite = useCallback(
    (site: ApiSite) => {
      updateSites((prev) => {
        if (site.siteId! < 0) {
          // new site
          return [...sites, { ...site, siteId: Math.floor(Math.random() * 100) + 10 }];
        } else {
          return prev.map((prev) => {
            if (prev.siteId === site.siteId) {
              console.log("Update site.");
              return site;
            } else {
              return prev;
            }
          });
        }
      });
    },
    [args],
  );

  return (
    <Story
      args={{
        ...args,
        sites: siteRefs,
        actions: { save: saveSite },
      }}
    />
  );
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
