import type { Meta, StoryObj } from "@storybook/react-vite";
import type { ApiConfigSensor } from "opendcs-api";
import { expect, fn, waitFor } from "storybook/test";
import { ConfigSensorsTable } from "./ConfigSensorsTable";

const sensors: ApiConfigSensor[] = [
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

const meta = {
  title: "Configs/ConfigSensorsTable",
  component: ConfigSensorsTable,
} satisfies Meta<typeof ConfigSensorsTable>;
export default meta;

type Story = StoryObj<typeof meta>;

export const Empty: Story = { args: { sensors: [], edit: false } };

export const ReadOnly: Story = {
  args: { sensors, edit: false },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await waitFor(() =>
      expect(canvas.getByText(i18n.t("configs:config_sensors"))).toBeInTheDocument(),
    );
    expect(canvas.getByText("Stage")).toBeInTheDocument();
    expect(canvas.getByText("Battery")).toBeInTheDocument();
  },
};

// Edit mode: edit / delete buttons appear for each sensor row.
export const EditModeRowActions: Story = {
  args: {
    sensors,
    edit: true,
    actions: { save: fn(), remove: fn() },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    await waitFor(() =>
      expect(
        canvas.getByRole("button", {
          name: i18n.t("configs:edit_sensor", { id: 1 }),
        }),
      ).toBeInTheDocument(),
    );
    expect(
      canvas.getByRole("button", {
        name: i18n.t("configs:delete_sensor_for", { id: 1 }),
      }),
    ).toBeInTheDocument();
  },
};

// Clicking edit on a sensor row expands the per-sensor editor — verifies
// renderSkeleton wiring so AppDataTable opens the detail.
export const EditOpensSensorDetail: Story = {
  args: {
    sensors,
    edit: true,
    actions: { save: fn(), remove: fn() },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await waitFor(() =>
      canvas.getByRole("button", {
        name: i18n.t("configs:edit_sensor", { id: 1 }),
      }),
    );
    await userEvent.click(editBtn);

    // The sensor editor renders the sensor-name input as a labelled control.
    await waitFor(() => {
      const nameInput = canvas.getByLabelText(
        i18n.t("configs:sensor_name"),
      ) as HTMLInputElement | null;
      expect(nameInput).not.toBeNull();
    });
  },
};
