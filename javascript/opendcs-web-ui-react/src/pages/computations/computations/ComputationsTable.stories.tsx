import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";
import { ComputationsTable, type TableComputationRef } from "./ComputationsTable";
import { ApiException } from "opendcs-api";
import type {
  ApiAlgorithm,
  ApiAppRef,
  ApiComputation,
  ApiComputationRef,
  ApiTsGroupRef,
  ApiAlgorithmRef,
} from "opendcs-api";
// eslint-disable-next-line storybook/use-storybook-testing-library
import { act } from "@testing-library/react";
import { expect, screen, waitFor, within } from "storybook/test";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { ArgsStoryFn } from "storybook/internal/types";
import type { RemoveAction, SaveAction } from "../../../util/Actions";
import { http, HttpResponse } from "msw";

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
    parmList: [
      {
        algoRoleName: "input",
        algoParmType: "i",
        siteName: "TESTSITE",
        dataType: "Flow",
        interval: "1Hour",
      },
      {
        algoRoleName: "output",
        algoParmType: "o",
        siteName: "TESTSITE",
        dataType: "Flow",
        interval: "1Hour",
        unitsAbbr: "cfs",
      },
    ],
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
    if (!comp) throw new Error(`No computation with id ${id}`);
    return { ...comp };
  };

const getAlgorithmFromList =
  (list: ApiAlgorithm[]) =>
  async (id: number): Promise<ApiAlgorithm> => {
    const algo = list.find((a) => a.algorithmId === id);
    if (!algo) throw new Error(`No algorithm with id ${id}`);
    return { ...algo };
  };

const sampleProcessOptions: ApiAppRef[] = [
  { appId: 200, appName: "compproc", appType: "ComputationProcess" },
  { appId: 201, appName: "routingproc", appType: "RoutingProcess" },
];

const sampleGroupOptions: ApiTsGroupRef[] = [
  { groupId: 5, groupName: "Daily", groupType: "Computation" },
  { groupId: 6, groupName: "Hourly", groupType: "Computation" },
];

type StoryArgs = {
  computations: TableComputationRef[];
  getComputation?: (computationId: number) => Promise<ApiComputation>;
  getAlgorithm?: (algorithmId: number) => Promise<ApiAlgorithm>;
  actions?: SaveAction<ApiComputation> & RemoveAction<number>;
};

let nextSavedId = 1000;

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
        return [...prev, { ...comp, computationId: nextSavedId++ }];
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
      processOptions={sampleProcessOptions}
      groupOptions={sampleGroupOptions}
    />
  );
};

const ComputationsTableWrapperWithSaveControl: React.FC<{
  initialComps: ApiComputation[];
  shouldFail: (comp: ApiComputation) => boolean;
  makeError?: () => unknown;
}> = ({
  initialComps,
  shouldFail,
  makeError = () => new Error("simulated save failure"),
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

  const saveComputation = useCallback(
    async (comp: ApiComputation) => {
      if (shouldFail(comp)) {
        throw makeError();
      }
      setLocalComps((prev) => {
        if (comp.computationId! < 0) {
          return [...prev, { ...comp, computationId: nextSavedId++ }];
        }
        return prev.map((c) => (c.computationId === comp.computationId ? comp : c));
      });
    },
    [shouldFail, makeError],
  );

  const removeComputation = useCallback((computationId: number) => {
    setLocalComps((prev) => prev.filter((c) => c.computationId !== computationId));
  }, []);

  return (
    <ComputationsTable
      computations={computationRefs}
      getComputation={localGetComputation}
      getAlgorithm={localGetAlgorithm}
      actions={{ save: saveComputation, remove: removeComputation }}
      processOptions={sampleProcessOptions}
      groupOptions={sampleGroupOptions}
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

export const AddMultipleComputations: Story = {
  args: { computations: toComputationRefs(sharedComputations) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:add_computation"),
    });

    await act(async () => userEvent.click(addBtn));
    await waitFor(() => {
      expect(canvas.queryByText("-1")).toBeInTheDocument();
    });

    await act(async () => userEvent.click(addBtn));
    await waitFor(() => {
      expect(canvas.queryByText("-2")).toBeInTheDocument();
    });

    // Both new rows coexist with distinct ids — no collision between them.
    expect(canvas.queryByText("-1")).toBeInTheDocument();
    expect(canvas.queryByText("-2")).toBeInTheDocument();
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
    const confirmBtn = await screen.findByRole("button", { name: "Delete" });
    await act(async () => userEvent.click(confirmBtn));

    await waitFor(() => {
      expect(canvas.queryByText("DailyStageMax")).not.toBeInTheDocument();
    });
  },
};

export const CopyComputation: Story = {
  args: { computations: toComputationRefs(sharedComputations) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const copyBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.copy_for", { id: 1 }),
    });
    await act(async () => userEvent.click(copyBtn));

    // A new local row with id -1 should appear in the table
    await waitFor(
      () => {
        expect(canvas.queryByText("-1")).toBeInTheDocument();
      },
      { timeout: 5000 },
    );
  },
};

// New rows that fail to save stay open in edit mode with the error surfaced,
// so the user doesn't lose what they typed (see AppDataTable's `isNewLocal`
// rethrow and Computation's save-error Alert).
export const AddComputationSaveFails: Story = {
  args: { computations: toComputationRefs(sharedComputations) },
  render: (args) => {
    const initialComps = sharedComputations.filter((c) =>
      (args.computations as ApiComputationRef[]).some(
        (ref) => ref.computationId === c.computationId,
      ),
    );
    return (
      <ComputationsTableWrapperWithSaveControl
        initialComps={initialComps}
        shouldFail={(comp) => (comp.computationId ?? 0) < 0}
      />
    );
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:add_computation"),
    });
    await act(async () => userEvent.click(addBtn));

    const nameInput = await canvas.findByRole(
      "textbox",
      { name: i18n.t("computations:editor.name") },
      { timeout: 5000 },
    );
    await userEvent.type(nameInput, "NewComputation");

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.save_for", { id: -1 }),
    });
    await act(async () => userEvent.click(saveBtn));

    expect(
      await canvas.findByText(i18n.t("computations:editor.save_error")),
    ).toBeInTheDocument();

    // Row stays in edit mode — the save button for id -1 is still present.
    expect(
      await canvas.findByRole("button", {
        name: i18n.t("computations:editor.save_for", { id: -1 }),
      }),
    ).toBeInTheDocument();
  },
};

export const AddComputationSaveFailsWithApiExceptionMessage: Story = {
  args: { computations: toComputationRefs(sharedComputations) },
  render: (args) => {
    const initialComps = sharedComputations.filter((c) =>
      (args.computations as ApiComputationRef[]).some(
        (ref) => ref.computationId === c.computationId,
      ),
    );
    return (
      <ComputationsTableWrapperWithSaveControl
        initialComps={initialComps}
        shouldFail={(comp) => (comp.computationId ?? 0) < 0}
        makeError={() =>
          new ApiException(
            409,
            "Conflict",
            { message: "Computation name already in use" },
            {},
          )
        }
      />
    );
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:add_computation"),
    });
    await act(async () => userEvent.click(addBtn));

    const nameInput = await canvas.findByRole(
      "textbox",
      { name: i18n.t("computations:editor.name") },
      { timeout: 5000 },
    );
    await userEvent.type(nameInput, "NewComputation");

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.save_for", { id: -1 }),
    });
    await act(async () => userEvent.click(saveBtn));

    expect(
      await canvas.findByText("Computation name already in use"),
    ).toBeInTheDocument();
    expect(
      canvas.queryByText(i18n.t("computations:editor.save_error")),
    ).not.toBeInTheDocument();
  },
};

// A structured Status body without a usable `message` field falls back to
// the generic save-error string, same as a plain Error.
export const AddComputationSaveFailsWithApiExceptionNoMessage: Story = {
  args: { computations: toComputationRefs(sharedComputations) },
  render: (args) => {
    const initialComps = sharedComputations.filter((c) =>
      (args.computations as ApiComputationRef[]).some(
        (ref) => ref.computationId === c.computationId,
      ),
    );
    return (
      <ComputationsTableWrapperWithSaveControl
        initialComps={initialComps}
        shouldFail={(comp) => (comp.computationId ?? 0) < 0}
        makeError={() => new ApiException(500, "Internal Server Error", {}, {})}
      />
    );
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:add_computation"),
    });
    await act(async () => userEvent.click(addBtn));

    const nameInput = await canvas.findByRole(
      "textbox",
      { name: i18n.t("computations:editor.name") },
      { timeout: 5000 },
    );
    await userEvent.type(nameInput, "NewComputation");

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.save_for", { id: -1 }),
    });
    await act(async () => userEvent.click(saveBtn));

    expect(
      await canvas.findByText(i18n.t("computations:editor.save_error")),
    ).toBeInTheDocument();
  },
};

// Existing rows that fail to save still close back to "show" mode — closing
// is safe since the row keeps its last-saved values — so no error is shown.
export const EditComputationSaveFails: Story = {
  args: { computations: toComputationRefs(sharedComputations) },
  render: (args) => {
    const initialComps = sharedComputations.filter((c) =>
      (args.computations as ApiComputationRef[]).some(
        (ref) => ref.computationId === c.computationId,
      ),
    );
    return (
      <ComputationsTableWrapperWithSaveControl
        initialComps={initialComps}
        shouldFail={() => true}
      />
    );
  },
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
    await act(async () => userEvent.click(saveBtn));

    await waitFor(async () => {
      const editBtnAfter = await canvas.findByRole("button", {
        name: i18n.t("computations:editor.edit_for", { id: 1 }),
      });
      expect(editBtnAfter).toBeInTheDocument();
    });

    expect(
      canvas.queryByText(i18n.t("computations:editor.save_error")),
    ).not.toBeInTheDocument();
  },
};

const mockAlgorithmRefs: ApiAlgorithmRef[] = [
  {
    algorithmId: 10,
    algorithmName: "AverageAlgorithm",
    execClass: "decodes.comp.AverageAlgorithm",
    description: "Computes average values.",
  },
];

export const AddFromAlgorithms: Story = {
  args: { computations: toComputationRefs(sharedComputations) },
  render: StoryRender,
  parameters: {
    msw: {
      handlers: {
        algorithmRefs: http.get("/odcsapi/algorithmrefs", () =>
          HttpResponse.json<ApiAlgorithmRef[]>(mockAlgorithmRefs),
        ),
      },
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    // "Add from algorithms" button lives in the DataTable header
    const addFromAlgoBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:add_from_algorithms.button"),
    });
    await act(async () => userEvent.click(addFromAlgoBtn));

    // Picker renders in a portal — find the chooser-style dialog
    const dialog = await screen.findByRole("dialog", {}, { timeout: 5000 });
    const modal = within(dialog);

    const algoRow = await modal.findByText("AverageAlgorithm");
    await act(async () => userEvent.click(algoRow));

    // Single-select chooser: confirm via the shared "Select" button
    const selectBtn = await modal.findByRole("button", {
      name: i18n.t("translation:select"),
    });
    await act(async () => userEvent.click(selectBtn));

    // A new local row with id -1 should appear in the table
    await waitFor(
      () => {
        expect(canvas.queryByText("-1")).toBeInTheDocument();
      },
      { timeout: 5000 },
    );
  },
};
