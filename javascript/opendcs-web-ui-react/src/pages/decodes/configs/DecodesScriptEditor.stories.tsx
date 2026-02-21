import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";

import {
  DecodesScriptEditor,
  DecodesScriptEditorProperties,
} from "./DecodesScriptEditor";
import { ApiConfigScriptDataOrderEnum, ApiDecodedMessage } from "opendcs-api";
import { WithUnits } from "../../../../.storybook/mock/WithUnits";
import { testDataSets } from "./Sample/Sample.stories";
import { ArgsStoryFn } from "storybook/internal/types";
import { useCallback } from "react";
import { fn } from "storybook/test";

const meta = {
  component: DecodesScriptEditor,
  decorators: [WithUnits],
} satisfies Meta<typeof DecodesScriptEditor>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: { sensors: [], decodeData: fn() },
};

const script = {
  name: "Test Script 1",
  dataOrder: ApiConfigScriptDataOrderEnum.D,
  headerType: "goes",
  scriptSensors: [
    { sensorNumber: 1, unitConverter: { ucId: 1 } },
    {
      sensorNumber: 2,
      unitConverter: { ucId: 3, toAbbr: "v", a: 10, algorithm: "linear" },
    },
  ],
  formatStatements: [
    { sequenceNum: 1, label: "Stage", format: "/,4f(s,a,6D' ', 1)>Volts" },
    { sequenceNum: 2, label: "Battery", format: "/,f(s,a,6D' ', 2)" },
    { sequenceNum: 3, label: "Battery", format: "/,f(s,a,6D' ', 2)" },
    { sequenceNum: 4, label: "Battery", format: "/,f(s,a,6D' ', 2)" },
    { sequenceNum: 5, label: "Battery", format: "/,f(s,a,6D' ', 2)" },
  ],
};

const sensors = [
  { sensorNumber: 1, sensorName: "Stage" },
  { sensorNumber: 2, sensorName: "Volt-Battery" },
];

export const WithScript: Story = {
  args: {
    script: script,
    sensors: sensors,
    decodeData: fn(),
  },
};

const WithDecodedMessage: ArgsStoryFn<ReactRenderer, DecodesScriptEditorProperties> = (
  args,
) => {
  const decodeData = useCallback((raw: string): ApiDecodedMessage => {
    args.decodeData(raw);
    return decodeData(raw);
  }, []);

  return (
    <>
      Paste desired test data into text area.
      <ul>
        {testDataSets.map((tds) => {
          return (
            <li key={tds.name}>
              {tds.name}:{" "}
              <pre style={{ border: "thin solid black" }}>
                <code>{tds.input}</code>
              </pre>
            </li>
          );
        })}
      </ul>
      <DecodesScriptEditor
        sensors={args.sensors}
        script={args.script}
        edit={args.edit}
        decodeData={decodeData}
      />
    </>
  );
};

export const WithScriptInEdit: Story = {
  args: {
    ...WithScript.args,
    edit: true,
  },
  render: WithDecodedMessage,
};
