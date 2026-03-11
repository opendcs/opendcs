import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";
import { Computation, type ComputationProperties } from "./Computation";
import { useCallback, useState } from "react";
import type { ApiComputation } from "opendcs-api";
import type { ArgsStoryFn } from "storybook/internal/types";
import { act } from "@testing-library/react";
import { expect } from "storybook/test";

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

const EditableComputation: React.FC<{ initialComp: ApiComputation }> = ({
  initialComp,
}) => {
  const [comp, setComp] = useState<ApiComputation>(initialComp);

  const save = useCallback((updated: ApiComputation) => {
    setComp(updated);
  }, []);

  return <Computation computation={comp} edit={true} actions={{ save }} />;
};

const EditableRender: ArgsStoryFn<ReactRenderer, ComputationProperties> = (args) => (
  <EditableComputation initialComp={args.computation as ApiComputation} />
);

export const ViewMode: Story = {
  args: {
    computation: sampleComputation,
    edit: false,
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const nameInput = await canvas.findByRole("textbox", {
      name: i18n.t("computations:editor.name"),
    });
    expect(nameInput).toHaveValue("DailyFlowAve");
    expect((nameInput as HTMLInputElement).readOnly).toBeTruthy();
  },
};

export const EditMode: Story = {
  args: {
    computation: sampleComputation,
    edit: true,
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
    await act(async () => userEvent.click(saveBtn));

    expect(nameInput).toHaveValue("DailyFlowAveUpdated");
  },
};
