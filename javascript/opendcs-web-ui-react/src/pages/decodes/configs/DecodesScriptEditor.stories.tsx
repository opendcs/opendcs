import type { Meta, StoryObj } from "@storybook/react-vite";

import { DecodesScriptEditor } from "./DecodesScriptEditor";
import { ApiConfigScriptDataOrderEnum } from "opendcs-api";
import { WithUnits } from "../../../../.storybook/mock/WithUnits";

const meta = {
  component: DecodesScriptEditor,
  decorators: [WithUnits],
} satisfies Meta<typeof DecodesScriptEditor>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: { sensors: [] },
};

export const WithScript: Story = {
  args: {
    script: {
      name: "Test Script 1",
      dataOrder: ApiConfigScriptDataOrderEnum.D,
      headerType: "goes",
      scriptSensors: [
        { sensorNumber: 1, unitConverter: { toAbbr: "ft" } },
        { sensorNumber: 2, unitConverter: { toAbbr: "v", a: 10, algorithm: "linear" } },
      ],
      formatStatements: [
        { sequenceNum: 1, label: "Stage", format: "/,4f(s,a,6D' ', 1)>Volts" },
        { sequenceNum: 2, label: "Battery", format: "/,f(s,a,6D' ', 2" },
        { sequenceNum: 3, label: "Battery", format: "/,f(s,a,6D' ', 2" },
        { sequenceNum: 4, label: "Battery", format: "/,f(s,a,6D' ', 2" },
        { sequenceNum: 5, label: "Battery", format: "/,f(s,a,6D' ', 2" },
      ],
    },
    sensors: [
      { sensorNumber: 1, sensorName: "Stage" },
      { sensorNumber: 2, sensorName: "Volt-Battery" },
    ],
  },
};
