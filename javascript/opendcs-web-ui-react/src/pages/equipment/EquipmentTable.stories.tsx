import type { Meta, StoryObj } from "@storybook/react-vite";
import { EquipmentTable, type TableEquipmentRef } from "./EquipmentTable";
import type { ApiEquipmentModel, ApiEquipmentModelRef } from "opendcs-api";
import { expect, waitFor } from "storybook/test";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { RemoveAction, SaveAction } from "../../util/Actions";

const meta = {
  component: EquipmentTable,
} satisfies Meta<typeof EquipmentTable>;

export default meta;

type Story = StoryObj<typeof meta>;

// ---------------------------------------------------------------------------
// Shared mock data
// ---------------------------------------------------------------------------

const sharedEquipment: ApiEquipmentModel[] = [
  {
    equipmentId: 1,
    name: "GOES-DCP-1",
    equipmentType: "goes",
    company: "Sutron",
    model: "9210B",
    description: "Sutron 9210B data logger",
    properties: { baudRate: "300" },
  },
  {
    equipmentId: 2,
    name: "Iridium-SBD-1",
    equipmentType: "iridium",
    company: "NAL Research",
    model: "A3LA-D",
    description: "NAL Research Iridium SBD modem",
    properties: {},
  },
];

const toRefs = (list: ApiEquipmentModel[]): ApiEquipmentModelRef[] =>
  list.map(({ equipmentId, name, equipmentType, company, model, description }) => ({
    equipmentId: equipmentId!,
    name,
    equipmentType,
    company,
    model,
    description,
  }));

const getFromList =
  (list: ApiEquipmentModel[]) =>
  async (id: number): Promise<ApiEquipmentModel> => {
    const eq = list.find((e) => e.equipmentId === id);
    if (!eq) throw new Error(`No equipment with id ${id}`);
    return { ...eq };
  };

// ---------------------------------------------------------------------------
// Stateful wrapper
// ---------------------------------------------------------------------------

let nextId = 100;

const EquipmentTableWrapper: React.FC<{ initialEquipment: ApiEquipmentModel[] }> = ({
  initialEquipment,
}) => {
  const [localList, setLocalList] = useState<ApiEquipmentModel[]>(initialEquipment);
  const localRef = useRef(localList);

  useEffect(() => {
    localRef.current = localList;
  }, [localList]);

  const refs = useMemo(() => toRefs(localList), [localList]);

  const localGet = useCallback((id: number) => getFromList(localRef.current)(id), []);

  const save = useCallback((eq: ApiEquipmentModel) => {
    setLocalList((prev) => {
      if ((eq.equipmentId ?? 0) < 0) {
        return [...prev, { ...eq, equipmentId: ++nextId }];
      }
      return prev.map((e) => (e.equipmentId === eq.equipmentId ? eq : e));
    });
  }, []);

  const remove = useCallback((equipmentId: number) => {
    setLocalList((prev) => prev.filter((e) => e.equipmentId !== equipmentId));
  }, []);

  return (
    <EquipmentTable
      equipment={refs}
      getEquipment={localGet}
      actions={{ save, remove }}
    />
  );
};

type StoryArgs = {
  equipment: TableEquipmentRef[];
  getEquipment?: (equipmentId: number) => Promise<ApiEquipmentModel>;
  actions?: SaveAction<ApiEquipmentModel> & RemoveAction<number>;
};

const StoryRender = (args: StoryArgs) => {
  const initial = sharedEquipment.filter((e) =>
    args.equipment.some((ref) => ref.equipmentId === e.equipmentId),
  );
  return <EquipmentTableWrapper initialEquipment={initial} />;
};

// ---------------------------------------------------------------------------
// Stories
// ---------------------------------------------------------------------------

export const Default: Story = {
  args: { equipment: [] },
  render: StoryRender,
  play: async ({ mount }) => {
    await mount();
  },
};

export const WithEquipment: Story = {
  args: { equipment: toRefs(sharedEquipment) },
  render: StoryRender,
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("equipment:edit_equipment", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    expect(editBtn).toBeInTheDocument();
  },
};

export const AddEquipmentThenCancel: Story = {
  args: { equipment: toRefs(sharedEquipment) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("equipment:add_equipment"),
    });
    await userEvent.click(addBtn);

    const newRow = await waitFor(() => canvas.queryByText("-1"));
    expect(newRow).toBeInTheDocument();

    const cancelBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("equipment:cancel_for", { id: -1 }) },
      { timeout: 5000 },
    );
    await userEvent.click(cancelBtn);

    await waitFor(() => {
      expect(canvas.queryByText("-1")).not.toBeInTheDocument();
    });
  },
};

export const EditEquipmentSave: Story = {
  args: { equipment: toRefs(sharedEquipment) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("equipment:edit_equipment", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    await userEvent.click(editBtn);

    const saveBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("equipment:save_equipment", { id: 1 }) },
      { timeout: 5000 },
    );
    expect(saveBtn).toBeInTheDocument();

    await userEvent.click(saveBtn);

    const editBtnAfter = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("equipment:edit_equipment", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    expect(editBtnAfter).toBeInTheDocument();
  },
};

export const DeleteEquipment: Story = {
  args: { equipment: toRefs(sharedEquipment) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const deleteBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("equipment:delete_for", { id: 2 }),
        }),
      { timeout: 5000 },
    );
    await userEvent.click(deleteBtn);

    await waitFor(() => {
      expect(canvas.queryByText("Iridium-SBD-1")).not.toBeInTheDocument();
    });
  },
};
