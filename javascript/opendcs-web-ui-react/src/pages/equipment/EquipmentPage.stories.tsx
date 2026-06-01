import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import type { ApiEquipmentModelRef } from "opendcs-api";
import { expect, waitFor } from "storybook/test";
import { EquipmentPage } from "./EquipmentPage";

const mockEquipmentRefs: ApiEquipmentModelRef[] = [
  {
    equipmentId: 1,
    name: "GOES-DCP-1",
    equipmentType: "goes",
    company: "Sutron",
    model: "9210B",
    description: "Sutron 9210B data logger",
  },
  {
    equipmentId: 2,
    name: "Iridium-SBD-1",
    equipmentType: "iridium",
    company: "NAL Research",
    model: "A3LA-D",
    description: "NAL Research Iridium SBD modem",
  },
];

const handlers = {
  equipmentRefs: http.get("/odcsapi/equipmentrefs", () =>
    HttpResponse.json<ApiEquipmentModelRef[]>(mockEquipmentRefs),
  ),
};

const meta = {
  component: EquipmentPage,
  parameters: { msw: { handlers } },
} satisfies Meta<typeof EquipmentPage>;

export default meta;

type Story = StoryObj<typeof meta>;

export const WithEquipment: Story = {
  play: async ({ mount }) => {
    const canvas = await mount();

    await waitFor(() => canvas.getByText("GOES-DCP-1"), { timeout: 5000 });
    expect(canvas.getByText("Iridium-SBD-1")).toBeInTheDocument();
  },
};

export const Empty: Story = {
  parameters: {
    msw: {
      handlers: {
        equipmentRefs: http.get("/odcsapi/equipmentrefs", () =>
          HttpResponse.json<ApiEquipmentModelRef[]>([]),
        ),
      },
    },
  },
  play: async ({ mount }) => {
    await mount();
  },
};
