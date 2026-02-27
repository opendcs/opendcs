import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";

import SensorConversions, { SensorConversionProperties } from "./SensorConversions";
import { ApiConfigScriptSensor, ApiConfigSensor } from "opendcs-api";
import { WithUnits } from "../../../../../.storybook/mock/WithUnits";
import { useCallback, useEffect, useState } from "react";
import { ArgsStoryFn } from "storybook/internal/types";
import { useArgs } from "storybook/internal/preview-api";

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

const Wrapper: React.FC<SensorConversionProperties> = ({
  scriptSensors,
  configSensors,
  edit,
  sensorChanged,
}) => {
  const [storySensors, setStorySensors] = useState<ApiConfigScriptSensor[]>([]);

  // keep the state local, if we call updateArgs then the component complete
  // rerenders and you can only type a single character at a time.
  useEffect(() => {
    setStorySensors(scriptSensors);
  }, []);

  const updateSensors = useCallback(
    (sensor: ApiConfigScriptSensor) => {
      sensorChanged?.(sensor);

      setStorySensors((prev) => {
        return prev.map((s) => {
          if (s.sensorNumber === sensor.sensorNumber) {
            return sensor;
          } else {
            return s;
          }
        });
      });
    },
    [setStorySensors],
  );

  return (
    <SensorConversions
      scriptSensors={storySensors}
      configSensors={configSensors}
      edit={edit}
      sensorChanged={updateSensors}
    />
  );
};

const StoryRender: ArgsStoryFn<ReactRenderer, SensorConversionProperties> = (args) => {
  const [{ scriptSensors }, _updateArgs] = useArgs();
  return (
    <Wrapper
      scriptSensors={scriptSensors}
      configSensors={args.configSensors}
      edit={args.edit}
      sensorChanged={args.sensorChanged}
    />
  );
};

export const WithSensorsEdit: Story = {
  args: {
    scriptSensors: sensors,
    configSensors: configSensors,
    edit: true,
  },
  render: StoryRender,
};
