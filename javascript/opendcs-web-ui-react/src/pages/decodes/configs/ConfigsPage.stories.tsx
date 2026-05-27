import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { http, HttpResponse } from "msw";
import type { ApiConfigRef, ApiPlatformConfig } from "opendcs-api";
import { expect, waitFor } from "storybook/test";
import { WithUnits } from "../../../../.storybook/mock/WithUnits";
import { ConfigsPage } from "./ConfigsPage";

const CONFIG_REFS: ApiConfigRef[] = [
  {
    configId: 101,
    name: "Standard CFG",
    numPlatforms: 3,
    description: "Default config",
  },
  {
    configId: 202,
    name: "Backup CFG",
    numPlatforms: 1,
    description: "Backup config",
  },
];

const FULL_CONFIGS: Record<number, ApiPlatformConfig> = {
  101: {
    configId: 101,
    name: "Standard CFG",
    numPlatforms: 3,
    description: "Default config",
    configSensors: [
      { sensorNumber: 1, sensorName: "Stage", dataTypes: { SHEF: "HG" } },
    ],
    scripts: [],
  },
  202: {
    configId: 202,
    name: "Backup CFG",
    numPlatforms: 1,
    description: "Backup config",
    configSensors: [],
    scripts: [],
  },
};

const baseHandlers = {
  configRefs: http.get("/odcsapi/configrefs", () =>
    HttpResponse.json<ApiConfigRef[]>(CONFIG_REFS),
  ),
  config: http.get("/odcsapi/config", ({ request }) => {
    const url = new URL(request.url);
    const id = Number(url.searchParams.get("configid"));
    return HttpResponse.json<ApiPlatformConfig>(FULL_CONFIGS[id] ?? {});
  }),
  postConfig: http.post("/odcsapi/config", async () =>
    HttpResponse.json<ApiPlatformConfig>({}),
  ),
  deleteConfig: http.delete("/odcsapi/config", () => HttpResponse.json({})),
};

const meta = {
  component: ConfigsPage,
  decorators: [WithUnits],
} satisfies Meta<typeof ConfigsPage>;

export default meta;

type Story = StoryObj<typeof meta>;

// Default render: list comes back, both configs show.
export const Default: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText("Standard CFG")).toBeInTheDocument();
    expect(await canvas.findByText("Backup CFG")).toBeInTheDocument();
  },
};

// Empty state: API returns nothing — caption still renders.
export const Empty: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        configRefs: http.get("/odcsapi/configrefs", () =>
          HttpResponse.json<ApiConfigRef[]>([]),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(await canvas.findByText(i18n.t("configs:configsTitle"))).toBeInTheDocument();
  },
};

// Click a row — detail card loads and shows the config's name input.
export const OpenConfigDetail: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const row = await canvas.findByText("Standard CFG");
    await act(async () => userEvent.click(row));
    await waitFor(async () => {
      const nameInput = (await canvas.findByLabelText(
        i18n.t("configs:name"),
      )) as HTMLInputElement;
      expect(nameInput.value).toEqual("Standard CFG");
    });
  },
};
