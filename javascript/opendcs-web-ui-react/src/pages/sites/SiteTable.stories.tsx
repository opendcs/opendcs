import type { Decorator, Meta, StoryObj } from "@storybook/react-vite";

import { SitesTable } from "./SitesTable";
import {
  ApiSite,
  ApiSiteRef,
} from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { expect, userEvent, waitFor } from "storybook/test";
import { use, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { act } from "@testing-library/react";

const meta = {
  component: SitesTable,
} satisfies Meta<typeof SitesTable>;

export default meta;

const sharedSites: ApiSite[] = [
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

const sharedSiteRefs: ApiSiteRef[] = toSiteRefs(sharedSites);

const getSite = async (id: number): Promise<ApiSite | undefined> => {
  return Promise.resolve(sharedSites.find((site) => site.siteId == id));
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

export const WithSites: Story = {
  args: {
    sites: sharedSiteRefs,
    getSite: getSite,
  },
  render: (args) => {
    const [storySites, updateSites] = useState<ApiSite[]>([]);
    const siteRefs = useMemo(() => toSiteRefs(storySites), [storySites]);

    useEffect(() => {
      const setupSites = async () => {
        const tmpSites: (ApiSite | undefined)[] = await Promise.all(
          args.sites.map(async (sf: ApiSiteRef) => args.getSite!(sf.siteId!)),
        );
        const filtered = tmpSites.filter((s) => s !== undefined);
        updateSites((_) => [...filtered]);
      };
      if (storySites.length === 0) {
        setupSites();
      }
    }, []);

    const saveSite = useCallback(
      (site: ApiSite) => {
        updateSites((prev) => {
          if (site.siteId! < 0) {
            // new site
            return [...prev, { ...site, siteId: Math.floor(Math.random() * 100) + 10 }];
          } else {
            return prev.map((prev) => {
              if (prev.siteId === site.siteId) {
                return {
                  ...site,
                };
              } else {
                return prev;
              }
            });
          }
        });
      },
      [storySites],
    );

    const localGetSite = useCallback(
      (id: number) => {
        const site = storySites.find((site) => site.siteId === id);
        return Promise.resolve(site);
      },
      [storySites],
    );

    return (
      <SitesTable
        sites={siteRefs}
        getSite={localGetSite}
        actions={{ save: saveSite }}
      />
    );
  },
  play: async ({ mount }) => {
    await mount();
  },
};

export const WithExistingAddNewSite: Story = {
  args: {
    sites: sharedSiteRefs,
    getSite: getSite,
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
