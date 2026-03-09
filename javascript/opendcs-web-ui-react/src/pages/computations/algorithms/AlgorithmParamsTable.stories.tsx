import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";
import {
  AlgorithmParamsTable,
  type AlgoParm,
  type AlgorithmParamsTableProps,
} from "./AlgorithmParamsTable";
import { expect, waitFor } from "storybook/test";
import { useCallback, useState } from "react";
import { act } from "@testing-library/react";
import type { ArgsStoryFn } from "storybook/internal/types";

const meta = {
  component: AlgorithmParamsTable,
} satisfies Meta<typeof AlgorithmParamsTable>;

export default meta;

type Story = StoryObj<typeof meta>;

const sampleParms: AlgoParm[] = [
  { roleName: "input1", parmType: "i" },
  { roleName: "output1", parmType: "o" },
  { roleName: "delta", parmType: "id" },
];

// Stateful render wrapper — keeps parms in local state so CRUD actions are visible
const EditableRender: ArgsStoryFn<ReactRenderer, AlgorithmParamsTableProps> = (
  args,
) => {
  const [parms, setParms] = useState<AlgoParm[]>(args.parms ?? []);

  const onAdd = useCallback((parm: AlgoParm) => {
    setParms((prev) => [...prev, parm]);
  }, []);

  const onRemove = useCallback((roleName: string) => {
    setParms((prev) => prev.filter((p) => p.roleName !== roleName));
  }, []);

  const onUpdate = useCallback((oldRoleName: string, newParm: AlgoParm) => {
    setParms((prev) => prev.map((p) => (p.roleName === oldRoleName ? newParm : p)));
  }, []);

  return (
    <AlgorithmParamsTable
      parms={parms}
      edit={true}
      onAdd={onAdd}
      onRemove={onRemove}
      onUpdate={onUpdate}
    />
  );
};

export const ViewEmpty: Story = {
  args: { parms: [], edit: false },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const addBtn = canvas.queryByRole("button", {
      name: i18n.t("algorithms:parms.add_parm"),
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
  },
};

export const EditMode: Story = {
  args: { parms: sampleParms },
  render: EditableRender,
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("algorithms:parms.add_parm"),
    });
    expect(addBtn).toBeInTheDocument();
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("algorithms:parms.edit_for", { name: "input1" }),
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
      name: i18n.t("algorithms:parms.add_parm"),
    });
    await userEvent.click(addBtn);

    const roleInput = await canvas.findByRole("textbox", {
      name: i18n.t("algorithms:parms.roleName_input"),
    });
    await userEvent.type(roleInput, "myInput");

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("algorithms:parms.save_parm"),
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
      name: i18n.t("algorithms:parms.add_parm"),
    });
    await userEvent.click(addBtn);

    // Attempt to save without filling in a role name
    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("algorithms:parms.save_parm"),
    });
    await act(async () => userEvent.click(saveBtn));

    await waitFor(
      () => {
        const roleInput = canvas.queryByRole("textbox", {
          name: i18n.t("algorithms:parms.roleName_input"),
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
      name: i18n.t("algorithms:parms.edit_for", { name: "input1" }),
    });
    await act(async () => userEvent.click(editBtn));

    const roleInput = await canvas.findByRole("textbox", {
      name: i18n.t("algorithms:parms.roleName_input"),
    });
    await userEvent.clear(roleInput);
    await userEvent.type(roleInput, "renamedInput");

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("algorithms:parms.save_parm"),
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
      name: i18n.t("algorithms:parms.delete_for", { name: "output1" }),
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
