import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";
import { ComputationsTable, type TableComputationRef } from "./ComputationsTable";
import type { ApiAlgorithm, ApiComputation, ApiComputationRef } from "opendcs-api";
import { act } from "@testing-library/react";
import { expect, waitFor } from "storybook/test";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { ArgsStoryFn } from "storybook/internal/types";
import type { RemoveAction, SaveAction } from "../../../util/Actions";

const meta = {
  component: ComputationsTable,
} satisfies Meta<typeof ComputationsTable>;

export default meta;

type Story = StoryObj<typeof meta>;

const sharedComputations: ApiComputation[] = [
  {
    computationId: 1,
    name: "DailyFlowAve",
    algorithmId: 10,
    algorithmName: "AverageAlgorithm",
    appId: 200,
    applicationName: "compproc",
    enabled: true,
    comment: "Computes daily average flow.",
    groupId: 5,
    groupName: "Daily",
    props: { output_units: "cfs" },
    parmList: [],
  },
  {
    computationId: 2,
    name: "DailyStageMax",
    algorithmId: 11,
    algorithmName: "MaxToDate",
    appId: 200,
    applicationName: "compproc",
    enabled: false,
    comment: "Computes daily max stage.",
    groupId: 5,
    groupName: "Daily",
    props: {},
    parmList: [],
  },
];

const sharedAlgorithms: ApiAlgorithm[] = [
  {
    algorithmId: 10,
    name: "AverageAlgorithm",
    execClass: "decodes.comp.AverageAlgorithm",
    description: "Computes average values.",
    props: {},
    parms: [
      { roleName: "input", parmType: "i" },
      { roleName: "output", parmType: "o" },
    ],
  },
  {
    algorithmId: 11,
    name: "MaxToDate",
    execClass: "decodes.comp.MaxToDate",
    description: "Computes max value to date.",
    props: {},
    parms: [
      { roleName: "input", parmType: "i" },
      { roleName: "output", parmType: "o" },
    ],
  },
];

const toComputationRefs = (comps: ApiComputation[]): ApiComputationRef[] =>
  comps.map(
    ({
      computationId,
      name,
      algorithmId,
      algorithmName,
      appId,
      applicationName,
      enabled,
      comment,
      groupId,
      groupName,
    }) => ({
      computationId,
      name,
      algorithmId,
      algorithmName,
      processId: appId,
      processName: applicationName,
      enabled,
      description: comment,
      groupId,
      groupName,
    }),
  );

const getComputationFromList =
  (list: ApiComputation[]) =>
  async (id: number): Promise<ApiComputation> => {
    const comp = list.find((c) => c.computationId === id);
    if (!comp) return Promise.reject(`No computation with id ${id}`);
    return { ...comp };
  };

const getAlgorithmFromList =
  (list: ApiAlgorithm[]) =>
  async (id: number): Promise<ApiAlgorithm> => {
    const algo = list.find((a) => a.algorithmId === id);
    if (!algo) return Promise.reject(`No algorithm with id ${id}`);
    return { ...algo };
  };

type StoryArgs = {
  computations: TableComputationRef[];
  getComputation?: (computationId: number) => Promise<ApiComputation>;
  getAlgorithm?: (algorithmId: number) => Promise<ApiAlgorithm>;
  actions?: SaveAction<ApiComputation> & RemoveAction<number>;
};

const ComputationsTableWrapper: React.FC<{ initialComps: ApiComputation[] }> = ({
  initialComps,
}) => {
  const [localComps, setLocalComps] = useState<ApiComputation[]>(initialComps);
  const localCompsRef = useRef(localComps);

  useEffect(() => {
    localCompsRef.current = localComps;
  }, [localComps]);

  const computationRefs = useMemo(() => toComputationRefs(localComps), [localComps]);

  const localGetComputation = useCallback(
    (id: number) => getComputationFromList(localCompsRef.current)(id),
    [],
  );
  const localGetAlgorithm = useCallback(
    (id: number) => getAlgorithmFromList(sharedAlgorithms)(id),
    [],
  );

  const saveComputation = useCallback((comp: ApiComputation) => {
    setLocalComps((prev) => {
      if (comp.computationId! < 0) {
        return [
          ...prev,
          { ...comp, computationId: Math.floor(Math.random() * 100) + 10 },
        ];
      }
      return prev.map((c) => (c.computationId === comp.computationId ? comp : c));
    });
  }, []);

  const removeComputation = useCallback((computationId: number) => {
    setLocalComps((prev) => prev.filter((c) => c.computationId !== computationId));
  }, []);

  return (
    <ComputationsTable
      computations={computationRefs}
      getComputation={localGetComputation}
      getAlgorithm={localGetAlgorithm}
      actions={{ save: saveComputation, remove: removeComputation }}
    />
  );
};

const StoryRender: ArgsStoryFn<ReactRenderer, StoryArgs> = (args) => {
  const initialComps = sharedComputations.filter((c) =>
    (args.computations as ApiComputationRef[]).some(
      (ref) => ref.computationId === c.computationId,
    ),
  );
  return <ComputationsTableWrapper initialComps={initialComps} />;
};

export const Default: Story = {
  args: { computations: [] },
  render: StoryRender,
  play: async ({ mount }) => {
    await mount();
  },
};

export const WithComputations: Story = {
  args: { computations: toComputationRefs(sharedComputations) },
  render: StoryRender,
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.edit_for", { id: 1 }),
    });
    expect(editBtn).toBeInTheDocument();
  },
};

export const AddComputationThenCancel: Story = {
  args: { computations: toComputationRefs(sharedComputations) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:add_computation"),
    });
    await act(async () => userEvent.click(addBtn));

    const newRow = await waitFor(() => canvas.queryByText("-1"));
    expect(newRow).toBeInTheDocument();

    const cancelBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("computations:editor.cancel_for", { id: -1 }) },
      { timeout: 5000 },
    );
    await act(async () => userEvent.click(cancelBtn));

    await waitFor(() => {
      expect(canvas.queryByText("-1")).not.toBeInTheDocument();
    });
  },
};

export const EditComputationSave: Story = {
  args: { computations: toComputationRefs(sharedComputations) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.edit_for", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));

    const saveBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("computations:editor.save_for", { id: 1 }) },
      { timeout: 5000 },
    );
    expect(saveBtn).toBeInTheDocument();

    await act(async () => userEvent.click(saveBtn));

    await waitFor(async () => {
      const editBtnAfter = await canvas.findByRole("button", {
        name: i18n.t("computations:editor.edit_for", { id: 1 }),
      });
      expect(editBtnAfter).toBeInTheDocument();
    });
  },
};

export const DeleteComputation: Story = {
  args: { computations: toComputationRefs(sharedComputations) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const deleteBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.delete_for", { id: 2 }),
    });
    await act(async () => userEvent.click(deleteBtn));

    await waitFor(() => {
      expect(canvas.queryByText("DailyStageMax")).not.toBeInTheDocument();
    });
  },
};
