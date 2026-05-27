import type { Meta, StoryObj } from "@storybook/react-vite";
import {
  ApiConfigScriptDataOrderEnum,
  type ApiDecodedMessage,
  type ApiPlatformConfig,
} from "opendcs-api";
import { expect, fn, waitFor } from "storybook/test";
import { WithUnits } from "../../../../.storybook/mock/WithUnits";
import { Config } from "./Config";

const fullConfig: ApiPlatformConfig = {
  configId: 101,
  name: "Standard CFG",
  numPlatforms: 3,
  description: "Default config",
  configSensors: [
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
    },
  ],
  scripts: [
    {
      name: "Main",
      headerType: "goes",
      dataOrder: ApiConfigScriptDataOrderEnum.D,
      formatStatements: [
        { sequenceNum: 1, label: "Stage", format: "/,4f(s,a,6D' ',1)" },
      ],
      scriptSensors: [{ sensorNumber: 1, unitConverter: { ucId: 1 } }],
    },
  ],
};

const noopDecode = (_raw: string): ApiDecodedMessage => ({}) as ApiDecodedMessage;

const meta = {
  title: "Configs/Config",
  component: Config,
  decorators: [WithUnits],
} satisfies Meta<typeof Config>;
export default meta;

type Story = StoryObj<typeof meta>;

// Read-only view: name, sensors and scripts are all visible.
export const ReadOnly: Story = {
  args: {
    details: { config: fullConfig },
    edit: false,
    decodeData: noopDecode,
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await waitFor(() =>
      expect(canvas.getByDisplayValue("Standard CFG")).toBeInTheDocument(),
    );
    expect(canvas.getByText(i18n.t("configs:config_sensors"))).toBeInTheDocument();
    expect(canvas.getByText(i18n.t("configs:scripts"))).toBeInTheDocument();
    expect(canvas.getByText("Stage")).toBeInTheDocument();
    expect(canvas.getByText("Main")).toBeInTheDocument();
  },
};

// Edit mode: save fires with the (possibly edited) config. The DetailFade
// wrapper keeps the real content `visibility: hidden` until its skeleton's
// CSS animation ends, so we wait for the input to become visible before
// interacting — without that wait userEvent fails with "could not be focused".
export const EditAndSave: Story = {
  args: {
    details: { config: fullConfig },
    edit: true,
    decodeData: noopDecode,
    actions: { save: fn(), cancel: fn() },
  },
  play: async ({ mount, parameters, userEvent, args }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const nameInput = (await waitFor(() => {
      const el = canvas.getByLabelText(i18n.t("configs:name")) as HTMLInputElement;
      expect(el).toBeVisible();
      return el;
    })) as HTMLInputElement;

    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, "Updated CFG");

    const saveBtn = canvas.getByRole("button", {
      name: i18n.t("configs:save_config", { id: 101 }),
    });
    await userEvent.click(saveBtn);

    await waitFor(() => expect(args.actions!.save).toHaveBeenCalled());
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const saved = (args.actions!.save as any).mock.calls[0][0] as ApiPlatformConfig;
    expect(saved.name).toEqual("Updated CFG");
  },
};
