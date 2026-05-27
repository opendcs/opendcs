import type { Meta, StoryObj } from "@storybook/react-vite";
import type { ApiConfigSensor, ApiPlatformSensor } from "opendcs-api";
import { expect, waitFor } from "storybook/test";
import { PlatformSensorsTable } from "./PlatformSensorsTable";

const configSensors: ApiConfigSensor[] = [
  {
    sensorNumber: 1,
    sensorName: "Stage",
    dataTypes: { SHEF: "HG" },
    recordingMode: "F",
    recordingInterval: 3600,
    absoluteMin: 0,
    absoluteMax: 100,
  },
  {
    sensorNumber: 2,
    sensorName: "Battery",
    dataTypes: { SHEF: "VB" },
    recordingMode: "F",
    recordingInterval: 3600,
  },
];

const overrideForSensor1: ApiPlatformSensor = {
  sensorNum: 1,
  min: 5,
  max: 95,
  actualSiteId: 22,
  usgsDdno: 4242,
};

const meta = {
  title: "Platforms/PlatformSensorsTable",
  component: PlatformSensorsTable,
} satisfies Meta<typeof PlatformSensorsTable>;
export default meta;

type Story = StoryObj<typeof meta>;

// Empty config means no rows — the table should show DataTables' empty state.
export const EmptyConfig: Story = {
  args: { configSensors: [], platformSensors: [], edit: false },
};

// One config sensor, no platform override — the row should show config's
// absoluteMin / absoluteMax (the fallback), and the action column shouldn't
// render in view mode.
export const ConfigOnlyNoOverride: Story = {
  args: {
    configSensors: [configSensors[0]],
    platformSensors: [],
    edit: false,
  },
  play: async ({ mount }) => {
    const canvas = await mount();
    await waitFor(() => expect(canvas.getByText("Stage")).toBeInTheDocument());
    // absoluteMax fallback "100" should appear since there's no override.max.
    expect(canvas.getByText("100")).toBeInTheDocument();
  },
};

// Override min/max should win over the config's absoluteMin/absoluteMax.
export const MergedWithOverride: Story = {
  args: {
    configSensors,
    platformSensors: [overrideForSensor1],
    edit: false,
  },
  play: async ({ mount }) => {
    const canvas = await mount();
    await waitFor(() => expect(canvas.getByText("Stage")).toBeInTheDocument());
    // Override values shown for sensor #1.
    expect(canvas.getByText("5")).toBeInTheDocument();
    expect(canvas.getByText("95")).toBeInTheDocument();
    expect(canvas.getByText("4242")).toBeInTheDocument();
  },
};

// Edit mode shows row actions; the clear-override (trash) action only shows
// for sensors that have an override.
export const EditModeRowActions: Story = {
  args: {
    configSensors,
    platformSensors: [overrideForSensor1],
    edit: true,
    actions: { save: () => undefined, remove: () => undefined },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    // Sensor #1 has an override → edit and clear actions both render.
    const edit1 = await waitFor(() =>
      canvas.getByRole("button", { name: i18n.t("platforms:edit_sensor", { id: 1 }) }),
    );
    expect(edit1).toBeInTheDocument();
    expect(
      canvas.getByRole("button", {
        name: i18n.t("platforms:delete_sensor_for", { id: 1 }),
      }),
    ).toBeInTheDocument();

    // Sensor #2 has no override → only edit, no clear-override.
    expect(
      canvas.getByRole("button", { name: i18n.t("platforms:edit_sensor", { id: 2 }) }),
    ).toBeInTheDocument();
    expect(
      canvas.queryByRole("button", {
        name: i18n.t("platforms:delete_sensor_for", { id: 2 }),
      }),
    ).not.toBeInTheDocument();
  },
};

// Clicking edit should expand the detail card with the per-sensor form.
export const ExpandEditOpensDetail: Story = {
  args: {
    configSensors,
    platformSensors: [overrideForSensor1],
    edit: true,
    actions: { save: () => undefined, remove: () => undefined },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await waitFor(() =>
      canvas.getByRole("button", { name: i18n.t("platforms:edit_sensor", { id: 1 }) }),
    );
    await userEvent.click(editBtn);

    // The detail card shows the override input row for min — it's editable in edit mode.
    await waitFor(() => {
      const min = canvas.getByLabelText(
        i18n.t("platforms:sensor_min"),
      ) as HTMLInputElement | null;
      expect(min).not.toBeNull();
    });
  },
};
