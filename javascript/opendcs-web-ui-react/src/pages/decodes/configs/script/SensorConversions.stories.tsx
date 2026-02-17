import type { Meta, StoryObj } from "@storybook/react-vite";

import SensorConversions from "./SensorConversions";
import {
  ApiConfigRef,
  ApiConfigScript,
  ApiConfigScriptSensor,
  ApiConfigSensor,
  ApiPlatformConfig,
} from "opendcs-api";
import { WithUnits } from "../../../../../.storybook/mock/WithUnits";

const meta = {
  component: SensorConversions,
  decorators: [WithUnits],
} satisfies Meta<typeof SensorConversions>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    scriptSensors: [],
    configSensors: {},
  },
};

const sensors: ApiConfigScriptSensor[] = [
  { sensorNumber: 1, unitConverter: { ucId: 1 } },
  { sensorNumber: 2, unitConverter: { ucId: 2 } },
  { sensorNumber: 3, unitConverter: { ucId: 3, algorithm: "linear", a: 2, b: 0 } },
];

const configSensors: { [k: number]: ApiConfigSensor } = {
  1: { sensorNumber: 1, sensorName: "Stage" },
  2: { sensorNumber: 2, sensorName: "Precip" },
  3: { sensorNumber: 3, sensorName: "Volts" },
};

const sensor: ApiConfigSensor = {};

export const WithSensors: Story = {
  args: {
    scriptSensors: sensors,
    configSensors: configSensors,
  },
};
