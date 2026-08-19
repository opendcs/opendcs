import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { expect, fn, screen, waitFor } from "storybook/test";
import { http, HttpResponse } from "msw";
import {
  ApiException,
  type ApiConfigRef,
  type ApiPlatform,
  type ApiPlatformConfig,
  type ApiSite,
  type ApiSiteRef,
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

const mockSiteRefs: ApiSiteRef[] = [
  { siteId: 22, publicName: "Beaver Creek", description: "Downstream gage" },
  // No siteId - the select handler must ignore this one rather than pointing
  // the platform at a site that cannot be resolved.
  { publicName: "Orphan Site", description: "Missing id" },
];

const siteRefsHandler = http.get("/odcsapi/siterefs", () =>
  HttpResponse.json<ApiSiteRef[]>(mockSiteRefs),
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

// Covers: savePlatform happy path - save resolves, no error alert is shown.
export const SaveSucceeds: Story = {
  args: {
    details,
    edit: true,
    actions: { save: fn(async () => {}), cancel: fn() },
  },
  play: async ({ args, mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("platforms:save_platform", { id: 1 }),
    });
    await act(async () => userEvent.click(saveBtn));

    await waitFor(() =>
      expect(args.actions?.save).toHaveBeenCalledWith(
        expect.objectContaining({ platformId: 1, name: "Alpha" }),
      ),
    );
    expect(canvas.queryByRole("alert")).not.toBeInTheDocument();
  },
};

// Covers: savePlatform catch - the API's own message is surfaced, and the
// alert's dismiss button clears it.
export const SaveErrorShowsApiMessage: Story = {
  args: {
    details,
    edit: true,
    actions: {
      save: fn(() =>
        Promise.reject(
          new ApiException(400, "Bad Request", { message: "Site is required" }, {}),
        ),
      ),
      cancel: fn(),
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("platforms:save_platform", { id: 1 }),
    });
    await act(async () => userEvent.click(saveBtn));

    const alert = await canvas.findByRole("alert");
    expect(alert).toHaveTextContent("Site is required");

    await act(async () =>
      userEvent.click(canvas.getByRole("button", { name: "Close alert" })),
    );
    await waitFor(() => expect(canvas.queryByRole("alert")).not.toBeInTheDocument());
  },
};

// Covers: savePlatform catch with a non-ApiException failure - generic message.
export const SaveErrorFallsBackToGenericMessage: Story = {
  args: {
    details,
    edit: true,
    actions: {
      save: fn(() => Promise.reject(new Error("network down"))),
      cancel: fn(),
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("platforms:save_platform", { id: 1 }),
    });
    await act(async () => userEvent.click(saveBtn));

    const alert = await canvas.findByRole("alert");
    expect(alert).toHaveTextContent(i18n.t("platforms:save_error"));
  },
};

// Covers: handleSelectSite - siteId present, so the site input and the
// platform name both pick up the chosen site's display name.
export const SelectSite: Story = {
  args: {
    details,
    edit: true,
    actions: { save: fn(async () => {}), cancel: fn() },
  },
  parameters: { msw: { handlers: { siteRefs: siteRefsHandler } } },
  play: async ({ args, mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const chooseSiteBtn = await canvas.findByRole("button", {
      name: i18n.t("platforms:select_site"),
    });
    await act(async () => userEvent.click(chooseSiteBtn));

    await screen.findByText(i18n.t("platforms:select_site"));
    await userEvent.click(await screen.findByText("Beaver Creek"));
    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:select") }),
    );

    await waitFor(() => {
      const siteInput = canvas.getByRole("textbox", { name: i18n.t("platforms:site") });
      expect((siteInput as HTMLInputElement).value).toBe("Beaver Creek");
    });

    // The name field mirrors the site, so a save carries the new value.
    const saveBtn = canvas.getByRole("button", {
      name: i18n.t("platforms:save_platform", { id: 1 }),
    });
    await act(async () => userEvent.click(saveBtn));
    await waitFor(() =>
      expect(args.actions?.save).toHaveBeenCalledWith(
        expect.objectContaining({ siteId: 22, name: "Beaver Creek" }),
      ),
    );
  },
};

// Covers: handleSelectSite early return - a site ref without an id leaves the
// platform's existing site untouched.
export const SelectSiteWithoutIdIsIgnored: Story = {
  args: {
    details,
    edit: true,
    actions: { save: fn(async () => {}), cancel: fn() },
  },
  parameters: { msw: { handlers: { siteRefs: siteRefsHandler } } },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const chooseSiteBtn = await canvas.findByRole("button", {
      name: i18n.t("platforms:select_site"),
    });
    await act(async () => userEvent.click(chooseSiteBtn));

    await screen.findByText(i18n.t("platforms:select_site"));
    await userEvent.click(await screen.findByText("Orphan Site"));
    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:select") }),
    );

    const siteInput = canvas.getByRole("textbox", { name: i18n.t("platforms:site") });
    await waitFor(() =>
      expect((siteInput as HTMLInputElement).value).toBe("Alder Springs"),
    );
  },
};

// Covers: the name field's `?? ""` fallback - a brand new platform has no
// name yet, and the controlled input must render empty rather than warn about
// switching from uncontrolled to controlled.
export const NewPlatformHasEmptyName: Story = {
  args: {
    details: { platform: { properties: {} } },
    edit: true,
    actions: { save: fn(async () => {}), cancel: fn() },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const nameInput = await canvas.findByRole("textbox", {
      name: i18n.t("platforms:name"),
    });
    expect(nameInput).toHaveValue("");
  },
};
