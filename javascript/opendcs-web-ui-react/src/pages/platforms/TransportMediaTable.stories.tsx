import type { Meta, StoryObj } from "@storybook/react-vite";
import type { ApiTransportMedium } from "opendcs-api";
import { TransportMediaTable } from "./TransportMediaTable";

const SAMPLE: ApiTransportMedium[] = [
  {
    mediumType: "GOES-SELFTIMED",
    mediumId: "CE31D030",
    scriptName: "ST",
    channelNum: 1,
    assignedTime: 0,
    transportWindow: 10,
    transportInterval: 3600,
    timezone: "UTC",
  },
  {
    mediumType: "GOES-RANDOM",
    mediumId: "CE31D030",
    scriptName: "RD",
    channelNum: 195,
  },
  {
    mediumType: "IRIDIUM",
    mediumId: "300434066009870",
    scriptName: "ST",
  },
];

const meta = {
  title: "Platforms/TransportMediaTable",
  component: TransportMediaTable,
} satisfies Meta<typeof TransportMediaTable>;
export default meta;

type Story = StoryObj<typeof meta>;

export const Populated: Story = {
  args: {
    media: SAMPLE,
    edit: false,
  },
};

export const EditMode: Story = {
  args: {
    media: SAMPLE,
    edit: true,
    actions: {
      save: () => undefined,
      remove: () => undefined,
    },
  },
};

export const Empty: Story = {
  args: {
    media: [],
    edit: false,
  },
};
