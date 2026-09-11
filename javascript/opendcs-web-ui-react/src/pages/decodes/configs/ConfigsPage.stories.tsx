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
  configRefs: http.get("/api/configrefs", () =>
    HttpResponse.json<ApiConfigRef[]>(CONFIG_REFS),
  ),
  config: http.get("/api/config", ({ request }) => {
    const url = new URL(request.url);
    const id = Number(url.searchParams.get("configid"));
    return HttpResponse.json<ApiPlatformConfig>(FULL_CONFIGS[id] ?? {});
  }),
  postConfig: http.post("/api/config", async () =>
    HttpResponse.json<ApiPlatformConfig>({}),
  ),
  deleteConfig: http.delete("/api/config", () => HttpResponse.json({})),
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
        configRefs: http.get("/api/configrefs", () =>
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

// Editing an existing config's name, saving, then reopening the row must show
// the value the server actually persisted rather than the pre-edit snapshot.
export const EditExistingConfigPersistsAfterSave: Story = {
  parameters: {
    msw: {
      handlers: (() => {
        let saved: ApiPlatformConfig | null = null;
        return {
          ...baseHandlers,
          config: http.get("/api/config", ({ request }) => {
            const url = new URL(request.url);
            const id = Number(url.searchParams.get("configid"));
            if (id === 101 && saved) return HttpResponse.json<ApiPlatformConfig>(saved);
            return HttpResponse.json<ApiPlatformConfig>(FULL_CONFIGS[id] ?? {});
          }),
          postConfig: http.post("/api/config", async ({ request }) => {
            saved = (await request.json()) as ApiPlatformConfig;
            return HttpResponse.json<ApiPlatformConfig>(saved);
          }),
        };
      })(),
    },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("configs:edit_config", { id: 101 }),
    });
    await act(async () => userEvent.click(editBtn));

    const nameInput = (await waitFor(() => {
      const el = canvas.getByLabelText(i18n.t("configs:name")) as HTMLInputElement;
      expect(el).toBeVisible();
      return el;
    })) as HTMLInputElement;

    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, "Renamed CFG");

    const saveBtn = canvas.getByRole("button", {
      name: i18n.t("configs:save_config", { id: 101 }),
    });
    await act(async () => userEvent.click(saveBtn));

    // Row collapses back to "show" once the save resolves.
    await waitFor(() =>
      expect(canvas.queryByLabelText(i18n.t("configs:name"))).toBeNull(),
    );

    const editBtnAgain = await canvas.findByRole("button", {
      name: i18n.t("configs:edit_config", { id: 101 }),
    });
    await act(async () => userEvent.click(editBtnAgain));

    await waitFor(() => {
      const el = canvas.getByLabelText(i18n.t("configs:name")) as HTMLInputElement;
      expect(el.value).toEqual("Renamed CFG");
    });
  },
};
