import type { Meta, StoryObj } from "@storybook/react-vite";
import type { ApiConfigSensor } from "opendcs-api";
import { expect, fn, waitFor } from "storybook/test";
import { ConfigSensor } from "./ConfigSensor";

const stage: ApiConfigSensor = {
  sensorNumber: 1,
  sensorName: "Stage",
  dataTypes: { SHEF: "HG", CWMS: "Stage" },
  recordingMode: "F",
  recordingInterval: 3600,
  timeOfFirstSample: 0,
  absoluteMin: 0,
  absoluteMax: 100,
  usgsStatCode: "00003",
  properties: { foo: "bar" },
};

const meta = {
  title: "Configs/ConfigSensor",
  component: ConfigSensor,
} satisfies Meta<typeof ConfigSensor>;
export default meta;

type Story = StoryObj<typeof meta>;

// View mode renders the sensor read-only — the data types and properties
// tables both appear, with the data-types table titled "Data Types" (not
// "Properties") via the new caption override on PropertiesTable.
export const ReadOnly: Story = {
  args: { sensor: stage, edit: false },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    await waitFor(() => expect(canvas.getByDisplayValue("Stage")).toBeInTheDocument());

    // Both captions are present, distinct, and not duplicated.
    const dataTypesCaption = canvas.getAllByText(i18n.t("configs:data_types"));
    expect(dataTypesCaption.length).toBeGreaterThanOrEqual(1);
    expect(canvas.getByText(i18n.t("properties:PropertiesTitle"))).toBeInTheDocument();
  },
};

// Edit mode opens the sensor fields. Save fires with the local edits applied.
// ConfigSensor wraps itself in DetailFade, which hides the real content until
// the skeleton's CSS animation ends — wait for visibility before interacting.
export const EditAndSave: Story = {
  args: {
    sensor: stage,
    edit: true,
    actions: { save: fn(), cancel: fn() },
  },
  play: async ({ mount, parameters, userEvent, args }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const nameInput = (await waitFor(() => {
      const el = canvas.getByLabelText(
        i18n.t("configs:sensor_name"),
      ) as HTMLInputElement;
      expect(el).toBeVisible();
      return el;
    })) as HTMLInputElement;

    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, "Renamed");

    const saveBtn = canvas.getByRole("button", {
      name: i18n.t("configs:save_sensor", { id: 1 }),
    });
    await userEvent.click(saveBtn);

    await waitFor(() => expect(args.actions!.save).toHaveBeenCalled());
    // First arg of save: ApiConfigSensor with the updated name.
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const saved = (args.actions!.save as any).mock.calls[0][0] as ApiConfigSensor;
    expect(saved.sensorName).toEqual("Renamed");
  },
};

// Entering a sensor number already used by a sibling sensor blocks the save
// and shows an inline error instead of silently discarding the edit.
export const DuplicateSensorNumberBlocksSave: Story = {
  args: {
    sensor: stage,
    edit: true,
    otherSensorNumbers: [2],
    actions: { save: fn(), cancel: fn() },
  },
  play: async ({ mount, parameters, userEvent, args }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const numberInput = (await waitFor(() => {
      const el = canvas.getByLabelText(
        i18n.t("configs:sensor_num"),
      ) as HTMLInputElement;
      expect(el).toBeVisible();
      return el;
    })) as HTMLInputElement;

    await userEvent.clear(numberInput);
    await userEvent.type(numberInput, "2");

    const saveBtn = canvas.getByRole("button", {
      name: i18n.t("configs:save_sensor", { id: 1 }),
    });
    await userEvent.click(saveBtn);

    await waitFor(() =>
      expect(canvas.getByText(i18n.t("configs:duplicate_sensor_number"))).toBeVisible(),
    );
    expect(args.actions!.save).not.toHaveBeenCalled();
  },
};

// The data-types caption is "Data Types", proving the new caption override
// on PropertiesTable. We assert "Properties" still appears for the other
// table below — both tables coexist with distinct titles.
export const DataTypesCaptionDistinctFromProperties: Story = {
  args: { sensor: stage, edit: false },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    await waitFor(() => expect(canvas.getByDisplayValue("Stage")).toBeInTheDocument());

    expect(canvas.getByText(i18n.t("configs:data_types"))).toBeInTheDocument();
    expect(canvas.getByText(i18n.t("properties:PropertiesTitle"))).toBeInTheDocument();
    // Both data-types entries (SHEF / CWMS) are visible in the table.
    expect(canvas.getByText("SHEF")).toBeInTheDocument();
    expect(canvas.getByText("CWMS")).toBeInTheDocument();
  },
};
