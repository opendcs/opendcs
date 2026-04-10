import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";

import {
  DecodesScriptEditor,
  DecodesScriptEditorProperties,
} from "./DecodesScriptEditor";
import {
  ApiConfigScript,
  ApiConfigScriptDataOrderEnum,
  ApiDecodedMessage,
} from "opendcs-api";
import { WithUnits } from "../../../../.storybook/mock/WithUnits";
import { decodeData, testDataSets } from "./Sample/Sample.stories";
import { ArgsStoryFn } from "storybook/internal/types";
import { useCallback } from "react";
// NOTE: so far this is the only test where the play function arg decomposition
// isn't behaving... no idea way, just rolling with it for now.
import { expect, fn, Mock, userEvent } from "storybook/test";

const meta = {
  component: DecodesScriptEditor,
  decorators: [WithUnits],
} satisfies Meta<typeof DecodesScriptEditor>;

export default meta;

type Story = StoryObj<typeof meta>;

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

export const Default: Story = {
  args: {
    sensors: sensors,
    decodeData: fn(),
    edit: true,
    actions: { save: fn(), cancel: fn() },
  },
};

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
  const localDecodeData = useCallback((raw: string): ApiDecodedMessage => {
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
        decodeData={localDecodeData}
        actions={args.actions}
      />
    </>
  );
};

export const WithScriptInEdit: Story = {
  args: {
    ...WithScript.args,
    edit: true,
    actions: {
      save: fn(),
      cancel: fn(),
    },
  },
  render: WithDecodedMessage,
};

export const CreateFromEmpty: Story = {
  args: {
    ...Default.args,
  },
  play: async ({ mount, parameters, args }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const mockSave = args.actions!.save! as Mock;
    const mockCancel = args.actions!.cancel! as Mock;
    mockSave.mockReset();
    mockCancel.mockReset();
    expect(await canvas.findByText("Stage")).toBeInTheDocument();

    const scriptNameInput = await canvas.findByRole("textbox", {
      name: "script name input",
    });
    console.log(userEvent);
    await userEvent.type(scriptNameInput, "Story Script");
    await new Promise((resolve) => setTimeout(resolve, 100));
    const label1 = await canvas.findByRole("textbox", {
      name: i18n.t("decodes:script_editor.format_statements.label_input", {
        sequence: 1,
      }),
    });

    await userEvent.click(label1);
    await userEvent.paste("Stage");
    await new Promise((resolve) => setTimeout(resolve, 100));
    const format1 = await canvas.findByRole("textbox", {
      name: i18n.t("decodes:script_editor.format_statements.format_input", {
        sequence: 1,
        label: "Stage",
      }),
    });
    await userEvent.click(format1);
    await userEvent.paste("/,4f(s,a,6D' ',1)");
    await new Promise((resolve) => setTimeout(resolve, 100));

    const unitsStage = await canvas.findByRole("combobox", {
      name: i18n.t("decodes:script_editor.sensors.unit", { name: "Stage" }),
    });
    await userEvent.selectOptions(unitsStage, "ft");

    const unitsVoltBatt = await canvas.findByRole("combobox", {
      name: i18n.t("decodes:script_editor.sensors.unit", { name: "Volt-Battery" }),
    });
    await userEvent.selectOptions(unitsVoltBatt, "v");

    const algoVoltBatt = await canvas.findByRole("combobox", {
      name: i18n.t("decodes:script_editor.sensors.algorithm", { name: "Volt-Battery" }),
    });
    await userEvent.selectOptions(algoVoltBatt, "linear");

    const a = await canvas.findByRole("textbox", {
      name: i18n.t("decodes:script_editor.sensors.coeff", {
        coeff: "a",
        name: "Volt-Battery",
      }),
    });
    await userEvent.click(a);
    await userEvent.paste("10.0");
    await new Promise((resolve) => setTimeout(resolve, 100));
    const b = await canvas.findByRole("textbox", {
      name: i18n.t("decodes:script_editor.sensors.coeff", {
        coeff: "b",
        name: "Volt-Battery",
      }),
    });
    await userEvent.click(b);
    await userEvent.paste("0.0");

    const save = await canvas.findByRole("button", {
      name: i18n.t("translation:save"),
    });
    await userEvent.click(save);
    expect(mockSave).toHaveBeenCalled();
    const savedScript = mockSave.mock.calls[0][0] as Partial<ApiConfigScript>;
    console.log(savedScript);
    expect(savedScript).not.toBeNull();
    expect(savedScript.name).toBe("Story Script");
    expect(savedScript.formatStatements).toHaveLength(1);
    expect(savedScript.formatStatements![0].label).toBe("Stage");
    expect(savedScript.formatStatements![0].format).toBe("/,4f(s,a,6D' ',1)");

    const sensorsV = savedScript.scriptSensors!.find((s) => s.sensorNumber === 2);
    expect(sensorsV?.unitConverter!.a).toBe(10);
    expect(sensorsV?.unitConverter!.b).toBe(0);

    const cancel = await canvas.findByRole("button", {
      name: i18n.t("translation:save"),
    });
    await userEvent.click(cancel);
    expect(mockSave).toHaveBeenCalled();
  },
};
