import type { Meta, StoryObj } from "@storybook/react-vite";

import { SitesTable } from "./index";
import { ApiSite } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";

const meta = {
  component: SitesTable,
} satisfies Meta<typeof SitesTable>;

export default meta;

type Story = StoryObj<typeof meta>;



export const Default: Story = {
  args: {
    sites: []
  },
};

const sites: ApiSite[] = [
  {
    description: "Test site 1",
    sitenames: {
      CWMS: "TEST SITE 1"
    },
    siteId: 1
  },
  {
    description: "Stargate command's backup location.",
    sitenames: {
      CWMS: "Alpha Site"
    },
    elevation: 5.21,
    elevUnits: "ft",
    publicName: "Clearly, this site is not public.",
    siteId: 2
  },
  {
      siteId: 3,
      description: "A test",
      properties: {
        "prop1": "value1"
      },
      sitenames: {
        "CWMS": "Alder Springs",
        "NWSHB5": "ALS"

      }
  }
];

export const WithSites: Story = {
  args: {
    sites: sites,
  
  }
}
