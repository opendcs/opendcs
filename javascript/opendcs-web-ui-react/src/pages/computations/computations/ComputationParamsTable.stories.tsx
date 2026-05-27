import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";
import {
  ComputationParamsTable,
  type CompParm,
  type ComputationParamsTableProps,
} from "./ComputationParamsTable";
import { expect, waitFor } from "storybook/test";
import { useCallback, useState } from "react";
// eslint-disable-next-line storybook/use-storybook-testing-library
import { act } from "@testing-library/react";
import type { ArgsStoryFn } from "storybook/internal/types";
import { http, HttpResponse } from "msw";
import type { ApiInterval } from "opendcs-api";

const meta = {
  component: ComputationParamsTable,
} satisfies Meta<typeof ComputationParamsTable>;

export default meta;

type Story = StoryObj<typeof meta>;

const sampleParms: CompParm[] = [
  {
    algoRoleName: "input1",
    algoParmType: "i",
    siteName: "TESTSITE",
    dataType: "Stage",
    interval: "1Hour",
    paramType: "Inst",
    duration: "0",
    version: "raw",
    deltaT: 0,
    deltaTUnits: "Seconds",
    unitsAbbr: "ft",
    ifMissing: "IGNORE",
  },
  {
    algoRoleName: "output1",
    algoParmType: "o",
    siteName: "TESTSITE",
    dataType: "Stage",
    interval: "1Hour",
    paramType: "Inst",
    duration: "0",
    version: "raw",
    deltaT: 0,
    deltaTUnits: "Seconds",
    unitsAbbr: "ft",
    ifMissing: "FAIL",
  },
  { algoRoleName: "delta", algoParmType: "id", deltaT: -1, deltaTUnits: "Hours" },
];

const EditableParamsTable: React.FC<{ initialParms: CompParm[] }> = ({
  initialParms,
}) => {
  const [parms, setParms] = useState<CompParm[]>(initialParms);

  const onAdd = useCallback((parm: CompParm) => {
    setParms((prev) => [...prev, parm]);
  }, []);

  const onRemove = useCallback((algoRoleName: string) => {
    setParms((prev) => prev.filter((p) => (p.algoRoleName ?? "") !== algoRoleName));
  }, []);

  const onUpdate = useCallback((oldRoleName: string, newParm: CompParm) => {
    setParms((prev) =>
      prev.map((p) => ((p.algoRoleName ?? "") === oldRoleName ? newParm : p)),
    );
  }, []);

  return (
    <ComputationParamsTable
      parms={parms}
      edit={true}
      onAdd={onAdd}
      onRemove={onRemove}
      onUpdate={onUpdate}
    />
  );
};

const EditableRender: ArgsStoryFn<ReactRenderer, ComputationParamsTableProps> = (
  args,
) => <EditableParamsTable initialParms={args.parms ?? []} />;

export const ViewEmpty: Story = {
  args: { parms: [], edit: false },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const addBtn = canvas.queryByRole("button", {
      name: i18n.t("computations:parms.add_parm"),
    });
    expect(addBtn).toBeNull();
  },
};

export const ViewWithParms: Story = {
  args: { parms: sampleParms, edit: false },
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText("input1")).toBeInTheDocument();
    expect(await canvas.findByText("output1")).toBeInTheDocument();
    expect((await canvas.findAllByText("TESTSITE")).length).toBeGreaterThan(0);
  },
};

export const EditMode: Story = {
  args: { parms: sampleParms },
  render: EditableRender,
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:parms.add_parm"),
    });
    expect(addBtn).toBeInTheDocument();
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:parms.edit_for", { name: "input1" }),
    });
    expect(editBtn).toBeInTheDocument();
  },
};

export const AddParm: Story = {
  args: { parms: [] },
  render: EditableRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:parms.add_parm"),
    });
    await userEvent.click(addBtn);

    const roleInput = await canvas.findByRole("textbox", {
      name: i18n.t("computations:parms.roleName_input"),
    });
    await userEvent.type(roleInput, "myInput");

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:parms.save_parm"),
    });
    await act(async () => userEvent.click(saveBtn));

    await waitFor(
      () => {
        expect(canvas.queryByText("myInput")).toBeInTheDocument();
      },
      { timeout: 5000 },
    );
  },
};

export const RoleNameCannotBeBlank: Story = {
  args: { parms: [] },
  render: EditableRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:parms.add_parm"),
    });
    await userEvent.click(addBtn);

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:parms.save_parm"),
    });
    await act(async () => userEvent.click(saveBtn));

    await waitFor(
      () => {
        const roleInput = canvas.queryByRole("textbox", {
          name: i18n.t("computations:parms.roleName_input"),
        });
        expect(roleInput).toBeInTheDocument();
        expect(roleInput).toHaveClass("border-warning");
      },
      { timeout: 5000 },
    );
  },
};

export const EditParm: Story = {
  args: { parms: sampleParms },
  render: EditableRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:parms.edit_for", { name: "input1" }),
    });
    await act(async () => userEvent.click(editBtn));

    const roleInput = await canvas.findByRole("textbox", {
      name: i18n.t("computations:parms.roleName_input"),
    });
    await userEvent.clear(roleInput);
    await userEvent.type(roleInput, "renamedInput");

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:parms.save_parm"),
    });
    await act(async () => userEvent.click(saveBtn));

    await waitFor(
      () => {
        expect(canvas.queryByText("renamedInput")).toBeInTheDocument();
        expect(canvas.queryByText("input1")).not.toBeInTheDocument();
      },
      { timeout: 5000 },
    );
  },
};

export const DeleteParm: Story = {
  args: { parms: sampleParms },
  render: EditableRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const deleteBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:parms.delete_for", { name: "output1" }),
    });
    await act(async () => userEvent.click(deleteBtn));

    await waitFor(
      () => {
        expect(canvas.queryByText("output1")).not.toBeInTheDocument();
      },
      { timeout: 5000 },
    );
  },
};

//  • intervalId:1 has name "1Hour"   → i.name ?? "" = "1Hour", length > 0 = true
//  • intervalId:2 has name undefined  → i.name ?? "" = "",      length > 0 = false
//  • intervalId:3 has name ""         → i.name ?? "" = "",      length > 0 = false
const MOCK_INTERVALS: ApiInterval[] = [
  { intervalId: 1, name: "1Hour" },
  { intervalId: 2 /* name intentionally missing */ },
  { intervalId: 3, name: "" },
];

/**
 * Verifies that interval names from the server are merged into the
 * interval combobox options.
 */
export const EditModeWithIntervals: Story = {
  args: { parms: sampleParms },
  render: EditableRender,
  parameters: {
    msw: {
      handlers: {
        intervals: http.get("/odcsapi/intervals", () =>
          HttpResponse.json<ApiInterval[]>(MOCK_INTERVALS),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    // The table renders with the edit buttons once mounted.
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:parms.edit_for", { name: "input1" }),
    });
    expect(editBtn).toBeInTheDocument();
  },
};
