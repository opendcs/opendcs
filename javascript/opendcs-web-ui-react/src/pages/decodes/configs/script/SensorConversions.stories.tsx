import type { Meta, StoryObj } from "@storybook/react-vite";

import SensorConversions from "./SensorConversions";
import { ApiConfigScript, ApiConfigScriptSensor, ApiConfigSensor } from "opendcs-api";

const meta = {
  component: SensorConversions,
} satisfies Meta<typeof SensorConversions>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    sensors: [],
  },
};

const sensors: ApiConfigScriptSensor[] = [
  { sensorNumber: 1, unitConverter: { ucId: 1 } },
  { sensorNumber: 2, unitConverter: { ucId: 2 } },
  { sensorNumber: 3, unitConverter: { ucId: 3, algorithm: "linear", a: 1, b: 0 } },
];

const sensor: ApiConfigSensor = {};

export const WithSensors: Story = {
  args: {
    sensors: sensors,
  },
};
