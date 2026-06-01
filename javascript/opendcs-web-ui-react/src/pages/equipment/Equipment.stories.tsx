import type { Meta, StoryObj } from "@storybook/react-vite";
import Equipment from "./Equipment";
import { expect, fn } from "storybook/test";
import type { ApiEquipmentModel } from "opendcs-api";

const meta = {
  component: Equipment,
} satisfies Meta<typeof Equipment>;

export default meta;

type Story = StoryObj<typeof meta>;

const equipment1: ApiEquipmentModel = {
  equipmentId: 1,
  name: "GOES-DCP-1",
  equipmentType: "goes",
  company: "Sutron",
  model: "9210B",
  description: "Sutron 9210B data logger",
  properties: { baudRate: "300" },
};

export const Default: Story = {
  args: {
    equipment: {},
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(await canvas.findByLabelText(i18n.t("equipment:name"))).toBeInTheDocument();
  },
};

export const WithEquipment: Story = {
  args: {
    equipment: equipment1,
  },
  play: async ({ mount }) => {
    await mount();
  },
};

export const WithEquipmentAsPromise: Story = {
  args: {
    equipment: Promise.resolve(equipment1),
  },
};

export const WithEquipmentInEdit: Story = {
  args: {
    equipment: equipment1,
    edit: true,
    actions: {
      save: fn((item: ApiEquipmentModel) => {
        console.log(item);
      }),
      cancel: fn(),
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(
      await canvas.findByRole("button", {
        name: i18n.t("equipment:save_equipment", { id: 1 }),
      }),
    ).toBeInTheDocument();
  },
};
