import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";
import { AlgorithmsTable, type TableAlgorithmRef } from "./AlgorithmsTable";
import type { ApiAlgorithm, ApiAlgorithmRef, ApiPropSpec } from "opendcs-api";
import { act } from "@testing-library/react";
import { expect, waitFor } from "storybook/test";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { ArgsStoryFn } from "storybook/internal/types";
import type { RemoveAction, SaveAction } from "../../../util/Actions";

const meta = {
  component: AlgorithmsTable,
} satisfies Meta<typeof AlgorithmsTable>;

export default meta;

type Story = StoryObj<typeof meta>;

// Shared mock data

const sharedAlgorithms: ApiAlgorithm[] = [
  {
    algorithmId: 1,
    name: "CopyAlgorithm",
    execClass: "decodes.comp.CopyAlgorithm",
    description: "Copies one timeseries to another.",
    props: {},
    parms: [
      { roleName: "input", parmType: "i" },
      { roleName: "output", parmType: "o" },
    ],
  },
  {
    algorithmId: 2,
    name: "ScalerAdder",
    execClass: "decodes.comp.ScalerAdder",
    description: "Multiplies by a constant and adds an offset.",
    props: { input_fu: "", output_fu: "" },
    parms: [
      { roleName: "input", parmType: "i" },
      { roleName: "output", parmType: "o" },
    ],
  },
];

const toAlgoRefs = (algos: ApiAlgorithm[]): ApiAlgorithmRef[] =>
  algos.map(({ algorithmId, name, execClass, description }) => ({
    algorithmId,
    algorithmName: name,
    execClass,
    description,
    numCompsUsing: 0,
  }));

const getAlgorithmFromList =
  (list: ApiAlgorithm[]) =>
  async (id: number): Promise<ApiAlgorithm> => {
    const algo = list.find((a) => a.algorithmId === id);
    if (!algo) return Promise.reject(`No algorithm with id ${id}`);
    return { ...algo };
  };

const getPropSpecs = async (_execClass: string): Promise<ApiPropSpec[]> => [];

type StoryArgs = {
  algorithms: TableAlgorithmRef[];
  getAlgorithm?: (algorithmId: number) => Promise<ApiAlgorithm>;
  getPropSpecs?: (execClass: string) => Promise<ApiPropSpec[]>;
  actions?: SaveAction<ApiAlgorithm> & RemoveAction<number>;
};

// Proper React component that owns algorithm state for editable stories
const AlgorithmsTableWrapper: React.FC<{ initialAlgos: ApiAlgorithm[] }> = ({
  initialAlgos,
}) => {
  const [localAlgos, setLocalAlgos] = useState<ApiAlgorithm[]>(initialAlgos);
  const localAlgosRef = useRef(localAlgos);

  useEffect(() => {
    localAlgosRef.current = localAlgos;
  }, [localAlgos]);

  const algoRefs = useMemo(() => toAlgoRefs(localAlgos), [localAlgos]);

  const localGetAlgorithm = useCallback(
    (id: number) => getAlgorithmFromList(localAlgosRef.current)(id),
    [],
  );

  const saveAlgorithm = useCallback((algo: ApiAlgorithm) => {
    setLocalAlgos((prev) => {
      if (algo.algorithmId! < 0) {
        return [
          ...prev,
          { ...algo, algorithmId: Math.floor(Math.random() * 100) + 10 },
        ];
      }
      return prev.map((a) => (a.algorithmId === algo.algorithmId ? algo : a));
    });
  }, []);

  const removeAlgorithm = useCallback((algorithmId: number) => {
    setLocalAlgos((prev) => prev.filter((a) => a.algorithmId !== algorithmId));
  }, []);

  return (
    <AlgorithmsTable
      algorithms={algoRefs}
      getAlgorithm={localGetAlgorithm}
      getPropSpecs={getPropSpecs}
      actions={{ save: saveAlgorithm, remove: removeAlgorithm }}
    />
  );
};

// Thin render function — seeds initial state from story args then delegates
const StoryRender: ArgsStoryFn<ReactRenderer, StoryArgs> = (args) => {
  const initialAlgos = sharedAlgorithms.filter((a) =>
    (args.algorithms as ApiAlgorithmRef[]).some(
      (ref) => ref.algorithmId === a.algorithmId,
    ),
  );
  return <AlgorithmsTableWrapper initialAlgos={initialAlgos} />;
};

export const Default: Story = {
  args: { algorithms: [] },
  render: StoryRender,
  play: async ({ mount }) => {
    await mount();
  },
};

export const WithAlgorithms: Story = {
  args: { algorithms: toAlgoRefs(sharedAlgorithms) },
  render: StoryRender,
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    // Edit button for algorithm 1 is visible (injected by drawCallback)
    const editBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("algorithms:editor.edit_for", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    expect(editBtn).toBeInTheDocument();
  },
};

export const AddAlgorithmThenCancel: Story = {
  args: { algorithms: toAlgoRefs(sharedAlgorithms) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("algorithms:add_algorithm"),
    });
    await act(async () => userEvent.click(addBtn));

    // A row with id "-1" should appear
    const newRow = await waitFor(() => canvas.queryByText("-1"));
    expect(newRow).toBeInTheDocument();

    const cancelBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("algorithms:editor.cancel_for", { id: -1 }) },
      { timeout: 5000 },
    );
    await act(async () => userEvent.click(cancelBtn));

    await waitFor(() => {
      expect(canvas.queryByText("-1")).not.toBeInTheDocument();
    });
  },
};

export const EditAlgorithmSave: Story = {
  args: { algorithms: toAlgoRefs(sharedAlgorithms) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("algorithms:editor.edit_for", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    await act(async () => userEvent.click(editBtn));

    // The algorithm editor opens; find the save button
    const saveBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("algorithms:editor.save_for", { id: 1 }) },
      { timeout: 5000 },
    );
    expect(saveBtn).toBeInTheDocument();

    await act(async () => userEvent.click(saveBtn));

    // After save the edit button re-appears (injected by drawCallback)
    const editBtnAfter = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("algorithms:editor.edit_for", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    expect(editBtnAfter).toBeInTheDocument();
  },
};

export const DeleteAlgorithm: Story = {
  args: { algorithms: toAlgoRefs(sharedAlgorithms) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const deleteBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("algorithms:editor.delete_for", { id: 2 }),
        }),
      { timeout: 5000 },
    );
    await act(async () => userEvent.click(deleteBtn));

    await waitFor(() => {
      expect(canvas.queryByText("ScalerAdder")).not.toBeInTheDocument();
    });
  },
};
