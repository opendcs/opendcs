import type { Meta, StoryObj } from "@storybook/react-vite";
import {
  ApiConfigScriptDataOrderEnum,
  type ApiConfigScript,
  type ApiConfigSensor,
  type ApiDecodedMessage,
} from "opendcs-api";
import { expect, fn, waitFor } from "storybook/test";
import { WithUnits } from "../../../../.storybook/mock/WithUnits";
import { ConfigScriptsTable } from "./ConfigScriptsTable";

const sensors: ApiConfigSensor[] = [
  { sensorNumber: 1, sensorName: "Stage" },
  { sensorNumber: 2, sensorName: "Battery" },
];

const scripts: ApiConfigScript[] = [
  {
    name: "Main",
    headerType: "goes",
    dataOrder: ApiConfigScriptDataOrderEnum.D,
    formatStatements: [{ sequenceNum: 1, label: "Stage", format: "/,4f(s,a,6D' ',1)" }],
    scriptSensors: [{ sensorNumber: 1, unitConverter: { ucId: 1 } }],
  },
  {
    name: "Backup",
    headerType: "other",
    dataOrder: ApiConfigScriptDataOrderEnum.A,
    formatStatements: [{ sequenceNum: 1 }],
    scriptSensors: [],
  },
];

const noopDecode = (_raw: string): ApiDecodedMessage => ({}) as ApiDecodedMessage;

const meta = {
  title: "Configs/ConfigScriptsTable",
  component: ConfigScriptsTable,
  decorators: [WithUnits],
} satisfies Meta<typeof ConfigScriptsTable>;
export default meta;

type Story = StoryObj<typeof meta>;

export const Empty: Story = {
  args: {
    scripts: [],
    configSensors: sensors,
    decodeData: noopDecode,
    edit: false,
  },
};

// View mode lists scripts and renders a "view" eye button (no edit/delete).
export const ReadOnly: Story = {
  args: {
    scripts,
    configSensors: sensors,
    decodeData: noopDecode,
    edit: false,
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await waitFor(() =>
      expect(canvas.getByText(i18n.t("configs:scripts"))).toBeInTheDocument(),
    );
    expect(canvas.getByText("Main")).toBeInTheDocument();
    expect(canvas.getByText("Backup")).toBeInTheDocument();
  },
};

// Edit mode: each script row exposes an edit pencil and a delete trash.
export const EditModeRowActions: Story = {
  args: {
    scripts,
    configSensors: sensors,
    decodeData: noopDecode,
    edit: true,
    actions: { save: fn(), remove: fn() },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await waitFor(() =>
      expect(
        canvas.getByRole("button", {
          name: i18n.t("configs:edit_script", { name: "Main" }),
        }),
      ).toBeInTheDocument(),
    );
    expect(
      canvas.getByRole("button", {
        name: i18n.t("configs:delete_script_for", { name: "Main" }),
      }),
    ).toBeInTheDocument();
  },
};

// Regression test for the renderSkeleton/hasDetail bug: clicking edit on a
// script row must expand the DecodesScriptEditor inline. Before the fix the
// click registered but the child row never opened because AppDataTable only
// runs syncChildRow when both renderDetail AND renderSkeleton are provided.
export const EditOpensScriptEditor: Story = {
  args: {
    scripts,
    configSensors: sensors,
    decodeData: noopDecode,
    edit: true,
    actions: { save: fn(), remove: fn() },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await waitFor(() =>
      canvas.getByRole("button", {
        name: i18n.t("configs:edit_script", { name: "Main" }),
      }),
    );
    await userEvent.click(editBtn);

    // The editor mounts and exposes the script-name input.
    await waitFor(
      () => {
        const nameInput = canvas.getByRole("textbox", { name: "script name input" });
        expect(nameInput).toBeInTheDocument();
      },
      { timeout: 5000 },
    );
  },
};

// View mode "view" button also opens the editor (read-only).
export const ViewButtonOpensReadOnlyEditor: Story = {
  args: {
    scripts,
    configSensors: sensors,
    decodeData: noopDecode,
    edit: false,
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const viewBtn = await waitFor(() =>
      canvas.getByRole("button", {
        name: i18n.t("configs:edit_script", { name: "Main" }),
      }),
    );
    await userEvent.click(viewBtn);

    await waitFor(
      () => {
        const nameInput = canvas.getByRole("textbox", { name: "script name input" });
        expect(nameInput).toBeInTheDocument();
      },
      { timeout: 5000 },
    );
  },
};
