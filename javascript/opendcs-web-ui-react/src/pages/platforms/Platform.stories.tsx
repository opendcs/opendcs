import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { expect, fn, screen, waitFor } from "storybook/test";
import { http, HttpResponse } from "msw";
import type {
  ApiConfigRef,
  ApiPlatform,
  ApiPlatformConfig,
  ApiSite,
} from "opendcs-api";
import { Platform, type PlatformDetails } from "./Platform";

const meta = {
  component: Platform,
} satisfies Meta<typeof Platform>;

export default meta;
type Story = StoryObj<typeof meta>;

const samplePlatform: ApiPlatform = {
  platformId: 1,
  name: "Alpha",
  agency: "USGS",
  siteId: 11,
  configId: 101,
  description: "Alpha platform",
  designator: "A",
  production: false,
  properties: {},
};

const sampleSite: ApiSite = {
  siteId: 11,
  sitenames: { cwms: "Alder Springs" },
  publicName: "Alder Springs",
};

const sampleConfig: ApiPlatformConfig = {
  configId: 101,
  name: "Standard CFG",
  configSensors: [{ sensorNum: 1, sensorName: "Stage" }],
};

const details: PlatformDetails = {
  platform: samplePlatform,
  site: sampleSite,
  config: sampleConfig,
};

const mockConfigRefs: ApiConfigRef[] = [
  { configId: 101, name: "Standard CFG", description: "Default config" },
  { configId: 202, name: "Backup CFG", description: "Backup config" },
];

const configRefsHandler = http.get("/odcsapi/configrefs", () =>
  HttpResponse.json<ApiConfigRef[]>(mockConfigRefs),
);

export const ViewMode: Story = {
  args: {
    details,
    edit: false,
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const designatorInput = await canvas.findByRole("textbox", {
      name: i18n.t("platforms:designator"),
    });
    expect(designatorInput).toHaveValue("A");
    expect((designatorInput as HTMLInputElement).readOnly).toBe(true);
  },
};

export const EditMode: Story = {
  args: {
    details,
    edit: true,
    actions: { save: fn(), cancel: fn() },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const designatorInput = await canvas.findByRole("textbox", {
      name: i18n.t("platforms:designator"),
    });
    expect((designatorInput as HTMLInputElement).readOnly).toBe(false);
    expect(
      canvas.getByRole("button", {
        name: i18n.t("platforms:save_platform", { id: 1 }),
      }),
    ).toBeInTheDocument();
  },
};

// Covers: handleSelectConfig → fetchConfig success + config.configSensors ?? [] (truthy branch)
export const SelectConfigWithSensors: Story = {
  args: {
    details,
    edit: true,
    actions: { save: fn(), cancel: fn() },
  },
  parameters: {
    msw: {
      handlers: {
        configRefs: configRefsHandler,
        config: http.get("/odcsapi/config", () =>
          HttpResponse.json<ApiPlatformConfig>({
            configId: 202,
            name: "Backup CFG",
            configSensors: [{ sensorNum: 2, sensorName: "Temp" }],
          }),
        ),
      },
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const chooseConfigBtn = await canvas.findByRole("button", {
      name: i18n.t("platforms:select_config"),
    });
    await act(async () => userEvent.click(chooseConfigBtn));

    await screen.findByText(i18n.t("platforms:select_config"));
    await userEvent.click(await screen.findByText("Backup CFG"));
    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:select") }),
    );

    await waitFor(() => {
      const configInput = canvas.getByRole("textbox", {
        name: i18n.t("platforms:config"),
      });
      expect((configInput as HTMLInputElement).value).toBe("Backup CFG");
    });
  },
};

// Covers: config.configSensors ?? [] (falsy branch — configSensors undefined → falls back to [])
export const SelectConfigWithoutSensors: Story = {
  args: {
    details,
    edit: true,
    actions: { save: fn(), cancel: fn() },
  },
  parameters: {
    msw: {
      handlers: {
        configRefs: configRefsHandler,
        config: http.get("/odcsapi/config", () =>
          HttpResponse.json<ApiPlatformConfig>({
            configId: 202,
            name: "Backup CFG",
          }),
        ),
      },
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const chooseConfigBtn = await canvas.findByRole("button", {
      name: i18n.t("platforms:select_config"),
    });
    await act(async () => userEvent.click(chooseConfigBtn));

    await screen.findByText(i18n.t("platforms:select_config"));
    await userEvent.click(await screen.findByText("Backup CFG"));
    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:select") }),
    );

    await waitFor(() => {
      const configInput = canvas.getByRole("textbox", {
        name: i18n.t("platforms:config"),
      });
      expect((configInput as HTMLInputElement).value).toBe("Backup CFG");
    });
  },
};

// Covers: handleSelectConfig → fetchConfig error → .catch(() => setConfigSensors([]))
export const SelectConfigFetchError: Story = {
  args: {
    details,
    edit: true,
    actions: { save: fn(), cancel: fn() },
  },
  parameters: {
    msw: {
      handlers: {
        configRefs: configRefsHandler,
        config: http.get("/odcsapi/config", () => HttpResponse.error()),
      },
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const chooseConfigBtn = await canvas.findByRole("button", {
      name: i18n.t("platforms:select_config"),
    });
    await act(async () => userEvent.click(chooseConfigBtn));

    await screen.findByText(i18n.t("platforms:select_config"));
    await userEvent.click(await screen.findByText("Backup CFG"));
    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:select") }),
    );

    // After a network error, the config name still updates (from the ref itself)
    // but sensors are cleared. No crash should occur.
    await waitFor(() => {
      const configInput = canvas.getByRole("textbox", {
        name: i18n.t("platforms:config"),
      });
      expect((configInput as HTMLInputElement).value).toBe("Backup CFG");
    });
  },
};
