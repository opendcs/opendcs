import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";
import {
  Computation,
  ComputationSkeleton,
  type ComputationProperties,
} from "./Computation";
import { useCallback, useState } from "react";
import type {
  ApiAlgorithm,
  ApiAlgorithmRef,
  ApiAppRef,
  ApiComputation,
  ApiTsGroupRef,
} from "opendcs-api";
import type { ArgsStoryFn } from "storybook/internal/types";
import { expect, fn, screen, waitFor } from "storybook/test";
import { http, HttpResponse } from "msw";

const meta = {
  component: Computation,
} satisfies Meta<typeof Computation>;

export default meta;

type Story = StoryObj<typeof meta>;

const sampleComputation: ApiComputation = {
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
};

const sampleAlgorithm: ApiAlgorithm = {
  algorithmId: 10,
  name: "AverageAlgorithm",
  execClass: "decodes.comp.AverageAlgorithm",
  description: "Computes average values.",
  props: {},
  parms: [
    { roleName: "input", parmType: "i" },
    { roleName: "output", parmType: "o" },
  ],
};

const sampleProcessOptions: ApiAppRef[] = [
  { appId: 200, appName: "compproc", appType: "ComputationProcess" },
  { appId: 201, appName: "routingproc", appType: "RoutingProcess" },
];

const sampleGroupOptions: ApiTsGroupRef[] = [
  { groupId: 5, groupName: "Daily", groupType: "Computation" },
  { groupId: 6, groupName: "Hourly", groupType: "Computation" },
];

const EditableComputation: React.FC<{
  initialComp: ApiComputation;
  algorithm?: ApiAlgorithm;
  processOptions?: ApiAppRef[];
  groupOptions?: ApiTsGroupRef[];
  getAlgorithm?: (algorithmId: number) => Promise<ApiAlgorithm>;
  onSave?: (comp: ApiComputation) => void;
  onCancel?: (id: number) => void;
}> = ({
  initialComp,
  algorithm,
  processOptions,
  groupOptions,
  getAlgorithm,
  onSave,
  onCancel,
}) => {
  const [comp, setComp] = useState<ApiComputation>(initialComp);

  const save = useCallback(
    (updated: ApiComputation) => {
      setComp(updated);
      onSave?.(updated);
    },
    [onSave],
  );

  const cancel = useCallback(
    (id: number) => {
      onCancel?.(id);
    },
    [onCancel],
  );

  return (
    <Computation
      computation={comp}
      algorithm={algorithm}
      edit={true}
      actions={{ save, cancel }}
      processOptions={processOptions}
      groupOptions={groupOptions}
      getAlgorithm={getAlgorithm}
    />
  );
};

const EditableRender: ArgsStoryFn<ReactRenderer, ComputationProperties> = (args) => (
  <EditableComputation
    initialComp={args.computation as ApiComputation}
    algorithm={args.algorithm as ApiAlgorithm}
    processOptions={args.processOptions}
    groupOptions={args.groupOptions}
  />
);

export const ViewMode: Story = {
  args: {
    computation: sampleComputation,
    algorithm: sampleAlgorithm,
    edit: false,
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const nameInput = await canvas.findByRole("textbox", {
      name: i18n.t("computations:editor.name"),
    });
    expect(nameInput).toHaveValue("DailyFlowAve");
    expect((nameInput as HTMLInputElement).readOnly).toBeTruthy();

    expect(await canvas.findByText("input")).toBeInTheDocument();
  },
};

export const EditMode: Story = {
  args: {
    computation: sampleComputation,
    algorithm: sampleAlgorithm,
    edit: true,
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  render: EditableRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const nameInput = await canvas.findByRole("textbox", {
      name: i18n.t("computations:editor.name"),
    });
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, "DailyFlowAveUpdated");

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.save_for", { id: 1 }),
    });
    await userEvent.click(saveBtn);

    expect(nameInput).toHaveValue("DailyFlowAveUpdated");
  },
};

export const SkeletonLoadingState: Story = {
  args: {
    computation: sampleComputation,
    edit: false,
  },
  render: () => (
    <div data-testid="skeleton-host">
      <ComputationSkeleton />
      <ComputationSkeleton edit={true} />
    </div>
  ),
  play: async ({ mount }) => {
    const canvas = await mount();
    const host = await canvas.findByTestId("skeleton-host");
    expect(host.querySelectorAll(".placeholder").length).toBeGreaterThan(0);
  },
};

export const SaveValidationErrorThenFix: Story = {
  args: {
    computation: { ...sampleComputation, name: "" } as ApiComputation,
    edit: true,
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  render: EditableRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.save_for", { id: 1 }),
    });
    await userEvent.click(saveBtn);

    const nameInput = await canvas.findByRole("textbox", {
      name: i18n.t("computations:editor.name"),
    });
    await waitFor(() => expect(nameInput).toHaveClass("is-invalid"));

    await userEvent.type(nameInput, "ProvidedName");
    await waitFor(() => expect(nameInput).not.toHaveClass("is-invalid"));

    await userEvent.click(saveBtn);
    expect(nameInput).toHaveValue("ProvidedName");
  },
};

const cancelMock = fn();

export const CancelInvokesAction: Story = {
  args: {
    computation: sampleComputation,
    edit: true,
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  render: (args) => (
    <EditableComputation
      initialComp={args.computation as ApiComputation}
      algorithm={args.algorithm as ApiAlgorithm}
      processOptions={args.processOptions}
      groupOptions={args.groupOptions}
      onCancel={cancelMock}
    />
  ),
  play: async ({ mount, parameters, userEvent }) => {
    cancelMock.mockClear();
    const canvas = await mount();
    const { i18n } = parameters;

    const cancelBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.cancel_for", { id: 1 }),
    });
    await userEvent.click(cancelBtn);

    expect(cancelMock).toHaveBeenCalledWith(1);
  },
};

export const EditFieldsAndToggleEnabled: Story = {
  args: {
    computation: sampleComputation,
    edit: true,
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  render: EditableRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const processSelect = await canvas.findByLabelText(
      i18n.t("computations:editor.applicationName"),
    );
    await userEvent.selectOptions(processSelect, "routingproc");
    expect((processSelect as HTMLSelectElement).value).toBe("routingproc");

    const groupSelect = await canvas.findByLabelText(
      i18n.t("computations:editor.groupName"),
    );
    await userEvent.selectOptions(groupSelect, "Hourly");
    expect((groupSelect as HTMLSelectElement).value).toBe("Hourly");

    const enabledCheck = await canvas.findByRole("checkbox", {
      name: i18n.t("computations:editor.enabled"),
    });
    await userEvent.click(enabledCheck);
    expect((enabledCheck as HTMLInputElement).checked).toBe(false);

    const commentBox = await canvas.findByRole("textbox", {
      name: i18n.t("computations:editor.description"),
    });
    await userEvent.clear(commentBox);
    await userEvent.type(commentBox, "Updated comment text");
    expect(commentBox).toHaveValue("Updated comment text");
  },
};

const algorithmRefsHandler = http.get("/odcsapi/algorithmrefs", () =>
  HttpResponse.json<ApiAlgorithmRef[]>([
    {
      algorithmId: 11,
      algorithmName: "MaxToDate",
      execClass: "decodes.comp.MaxToDate",
      description: "Reference description for MaxToDate.",
      numCompsUsing: 0,
    },
  ]),
);

const fullMaxAlgorithm: ApiAlgorithm = {
  algorithmId: 11,
  name: "MaxToDate",
  execClass: "decodes.comp.MaxToDate",
  description: "Full description for MaxToDate.",
  props: { defaultProp: "fromAlgorithm" },
  parms: [
    { roleName: "newInput", parmType: "i" },
    { roleName: "newOutput", parmType: "o" },
  ],
};

const selectAlgorithmArgs = {
  computation: { ...sampleComputation, algorithmId: undefined, algorithmName: "" },
  edit: true,
  processOptions: sampleProcessOptions,
  groupOptions: sampleGroupOptions,
} satisfies ComputationProperties;

const selectAlgorithmParameters = {
  msw: { handlers: { algorithmRefs: algorithmRefsHandler } },
};

const renderSelectAlgorithm =
  (
    getAlgorithm?: (algorithmId: number) => Promise<ApiAlgorithm>,
  ): ArgsStoryFn<ReactRenderer, ComputationProperties> =>
  (args) => (
    <EditableComputation
      initialComp={args.computation as ApiComputation}
      algorithm={undefined}
      processOptions={args.processOptions}
      groupOptions={args.groupOptions}
      getAlgorithm={getAlgorithm}
    />
  );

const playSelectMaxToDate: NonNullable<Story["play"]> = async ({
  mount,
  parameters,
  userEvent,
}) => {
  const canvas = await mount();
  const { i18n } = parameters;

  await userEvent.click(
    await canvas.findByRole("button", { name: "Select Algorithm" }),
  );

  await screen.findByText(i18n.t("computations:editor.select_algorithm_title"));
  await userEvent.click(await screen.findByText("MaxToDate"));
  await userEvent.click(
    await screen.findByRole("button", { name: i18n.t("translation:select") }),
  );

  const algorithmInput = await canvas.findByRole("textbox", {
    name: i18n.t("computations:editor.algorithmName"),
  });
  await waitFor(() => expect(algorithmInput).toHaveValue("MaxToDate"));
};

export const SelectAlgorithmWithFullFetch: Story = {
  args: selectAlgorithmArgs,
  parameters: selectAlgorithmParameters,
  render: renderSelectAlgorithm(async () => fullMaxAlgorithm),
  play: playSelectMaxToDate,
};

export const SelectAlgorithmWithoutGetAlgorithm: Story = {
  args: selectAlgorithmArgs,
  parameters: selectAlgorithmParameters,
  render: renderSelectAlgorithm(),
  play: playSelectMaxToDate,
};

export const SelectAlgorithmGetAlgorithmFails: Story = {
  args: selectAlgorithmArgs,
  parameters: selectAlgorithmParameters,
  render: renderSelectAlgorithm(async () => {
    throw new Error("simulated failure");
  }),
  play: playSelectMaxToDate,
};

export const DefaultOptionalProps: Story = {
  args: {
    computation: sampleComputation,
    edit: false,
  },
  render: (args) => (
    <Computation
      computation={args.computation as ApiComputation}
      algorithm={undefined}
      edit={false}
    />
  ),
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const nameInput = await canvas.findByRole("textbox", {
      name: i18n.t("computations:editor.name"),
    });
    expect(nameInput).toHaveValue("DailyFlowAve");
  },
};

const algorithmWithBlankRoles: ApiAlgorithm = {
  algorithmId: 12,
  name: "BlankRoleAlgorithm",
  execClass: "decodes.comp.Blank",
  description: "Algorithm with a blank-role parm.",
  props: {},
  parms: [
    { roleName: "validInput", parmType: "i" },
    { roleName: "   ", parmType: "i" },
  ],
};

const compWithExtraParm: ApiComputation = {
  ...sampleComputation,
  algorithmId: 12,
  algorithmName: "BlankRoleAlgorithm",
  parmList: [
    {
      algoRoleName: "extraExisting",
      algoParmType: "o",
      siteName: "TESTSITE",
      dataType: "Stage",
      interval: "1Hour",
    },
  ],
};

export const MergesExistingParmsAndIgnoresBlankRoles: Story = {
  args: {
    computation: compWithExtraParm,
    algorithm: algorithmWithBlankRoles,
    edit: false,
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  play: async ({ mount }) => {
    const canvas = await mount();

    expect(await canvas.findByText("validInput")).toBeInTheDocument();
    expect(await canvas.findByText("extraExisting")).toBeInTheDocument();
  },
};

export const CancelAlgorithmModal: Story = {
  args: {
    computation: { ...sampleComputation, algorithmId: undefined, algorithmName: "" },
    edit: true,
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  parameters: {
    msw: { handlers: { algorithmRefs: algorithmRefsHandler } },
  },
  render: (args) => (
    <EditableComputation
      initialComp={args.computation as ApiComputation}
      algorithm={undefined}
      processOptions={args.processOptions}
      groupOptions={args.groupOptions}
    />
  ),
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const selectBtn = await canvas.findByRole("button", {
      name: "Select Algorithm",
    });
    await userEvent.click(selectBtn);

    const cancelBtn = await screen.findByRole("button", {
      name: i18n.t("translation:cancel"),
    });
    await userEvent.click(cancelBtn);

    await waitFor(() =>
      expect(
        screen.queryByText(i18n.t("computations:editor.select_algorithm_title")),
      ).not.toBeInTheDocument(),
    );
  },
};

export const AddAndRemoveProperty: Story = {
  args: {
    computation: sampleComputation,
    edit: true,
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  render: EditableRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const addPropBtn = await canvas.findByRole("button", {
      name: i18n.t("properties:add_prop"),
    });
    await userEvent.click(addPropBtn);

    const nameInput = await canvas.findByRole(
      "textbox",
      { name: /name input for property/i },
      { timeout: 5000 },
    );
    await userEvent.type(nameInput, "new_prop");
    const valueInput = await canvas.findByRole("textbox", {
      name: /value input for property/i,
    });
    await userEvent.type(valueInput, "new_value");

    const savePropBtn = await canvas.findByRole("button", {
      name: /save property named/i,
    });
    await userEvent.click(savePropBtn);

    await waitFor(() => expect(canvas.queryByText("new_prop")).toBeInTheDocument());

    const deletePropBtn = await canvas.findByRole("button", {
      name: i18n.t("properties:delete_prop", { name: "new_prop" }),
    });
    await userEvent.click(deletePropBtn);

    await waitFor(() => expect(canvas.queryByText("new_prop")).not.toBeInTheDocument());
  },
};

// Read path: API effectiveStart/End fields parsed into SinceUntilEditor display.
export const EffectiveTimeParsed: Story = {
  args: {
    computation: {
      ...sampleComputation,
      effectiveStartType: "Now -",
      effectiveStartInterval: "1 hour",
      effectiveEndType: "No Limit",
    },
    edit: false,
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const startSelect = (await canvas.findByRole("combobox", {
      name: i18n.t("computations:editor.effective_start"),
    })) as HTMLSelectElement;
    expect(startSelect.value).toEqual("nowMinus");

    const amount = (await canvas.findByLabelText(
      i18n.t("computations:editor.interval"),
    )) as HTMLInputElement;
    expect(amount.value).toEqual("1 hour");

    const endSelect = (await canvas.findByRole("combobox", {
      name: i18n.t("computations:editor.effective_end"),
    })) as HTMLSelectElement;
    expect(endSelect.value).toEqual("noLimit");
  },
};

// Write path: switch Effective Start to Calendar → datetime input appears.
export const EditEffectiveStartToCalendar: Story = {
  args: {
    computation: {
      ...sampleComputation,
      effectiveStartType: "Now -",
      effectiveStartInterval: "2 hours",
    },
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  render: EditableRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const startSelect = await canvas.findByRole("combobox", {
      name: i18n.t("computations:editor.effective_start"),
    });
    await userEvent.selectOptions(startSelect, "calendar");

    const calInput = await canvas.findByLabelText(
      i18n.t("computations:editor.calendar"),
    );
    expect(calInput).toBeInTheDocument();
  },
};

export const SaveErrorShowsDismissibleAlert: Story = {
  args: {
    computation: sampleComputation,
    edit: true,
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  render: (args) => (
    <Computation
      computation={args.computation as ApiComputation}
      algorithm={args.algorithm as ApiAlgorithm}
      edit={true}
      actions={{
        save: async () => {
          throw new Error("simulated save failure");
        },
      }}
      processOptions={args.processOptions}
      groupOptions={args.groupOptions}
    />
  ),
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.save_for", { id: 1 }),
    });
    await userEvent.click(saveBtn);

    const alert = await canvas.findByText(i18n.t("computations:editor.save_error"));
    expect(alert).toBeInTheDocument();

    const dismissBtn = await canvas.findByRole("button", { name: /close/i });
    await userEvent.click(dismissBtn);

    await waitFor(() =>
      expect(
        canvas.queryByText(i18n.t("computations:editor.save_error")),
      ).not.toBeInTheDocument(),
    );
  },
};

export const ComputationFromPromise: Story = {
  args: {
    computation: sampleComputation,
    edit: false,
    processOptions: sampleProcessOptions,
    groupOptions: sampleGroupOptions,
  },
  render: (args) => (
    <Computation
      computation={Promise.resolve(args.computation as ApiComputation)}
      algorithm={Promise.resolve(args.algorithm as ApiAlgorithm | undefined)}
      edit={false}
      processOptions={args.processOptions}
      groupOptions={args.groupOptions}
    />
  ),
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const nameInput = await canvas.findByRole("textbox", {
      name: i18n.t("computations:editor.name"),
    });
    expect(nameInput).toHaveValue("DailyFlowAve");
  },
};
