import type { Meta, StoryObj } from "@storybook/react-vite";
import { Computations } from "./index";
import { http, HttpResponse } from "msw";
import type {
  ApiAlgorithm,
  ApiAppRef,
  ApiComputation,
  ApiComputationRef,
  ApiTsGroupRef,
} from "opendcs-api";
import { act } from "react";
import { expect, fn, waitFor } from "storybook/test";
import {
  ApiContext,
  defaultValue as apiDefault,
} from "../../../contexts/app/ApiContext";

const WithOrg: React.FC<{ children: React.ReactNode; org?: string }> = ({
  children,
  org = "SPK",
}) => <ApiContext value={{ ...apiDefault, org, setOrg: fn() }}>{children}</ApiContext>;

const meta = {
  component: Computations,
} satisfies Meta<typeof Computations>;

export default meta;

type Story = StoryObj<typeof meta>;

const mockComputations: ApiComputation[] = [
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

const mockComputationRefs: ApiComputationRef[] = mockComputations.map(
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

const mockAlgorithms: ApiAlgorithm[] = [
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

const mockAppRefs: ApiAppRef[] = [
  { appId: 200, appName: "compproc", appType: "ComputationProcess" },
  { appId: 201, appName: "routingproc", appType: "RoutingProcess" },
];

const mockGroupRefs: ApiTsGroupRef[] = [
  { groupId: 5, groupName: "Daily", groupType: "Computation" },
  { groupId: 6, groupName: "Hourly", groupType: "Computation" },
];

const computationHandlers = {
  computationRefs: http.get("/odcsapi/computationrefs", () =>
    HttpResponse.json<ApiComputationRef[]>(mockComputationRefs),
  ),
  computation: http.get("/odcsapi/computation", ({ request }) => {
    const url = new URL(request.url);
    const id = parseInt(url.searchParams.get("computationid") ?? "-1");
    const comp = mockComputations.find((c) => c.computationId === id);
    if (!comp) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json(comp);
  }),
  saveComputation: http.post("/odcsapi/computation", async ({ request }) => {
    const body = (await request.json()) as ApiComputation;
    return HttpResponse.json({ ...body, computationId: body.computationId ?? 99 });
  }),
  deleteComputation: http.delete("/odcsapi/computation", () => {
    return new HttpResponse(null, { status: 204 });
  }),
  algorithm: http.get("/odcsapi/algorithm", ({ request }) => {
    const url = new URL(request.url);
    const id = parseInt(url.searchParams.get("algorithmid") ?? "-1");
    const algo = mockAlgorithms.find((a) => a.algorithmId === id);
    if (!algo) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json(algo);
  }),
  appRefs: http.get("/odcsapi/apprefs", () =>
    HttpResponse.json<ApiAppRef[]>(mockAppRefs),
  ),
  groupRefs: http.get("/odcsapi/tsgrouprefs", () =>
    HttpResponse.json<ApiTsGroupRef[]>(mockGroupRefs),
  ),
};

export const Default: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: computationHandlers,
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const computationCell = await canvas.findByText("DailyFlowAve");
    expect(computationCell).toBeInTheDocument();

    await act(async () => computationCell.click());

    const algorithmInput = await canvas.findByRole(
      "textbox",
      { name: i18n.t("computations:editor.algorithmName") },
      { timeout: 5000 },
    );
    expect((algorithmInput as HTMLInputElement).readOnly).toBeTruthy();
    expect(algorithmInput).toHaveValue("AverageAlgorithm");
  },
};

export const EditAndSave: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: computationHandlers,
    },
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

export const AddAndCancel: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: computationHandlers,
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    await canvas.findByText("DailyFlowAve");

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

const orgScopedHandlers = {
  appRefs: http.get("/odcsapi/apprefs", () =>
    HttpResponse.json<ApiAppRef[]>(mockAppRefs),
  ),
  groupRefs: http.get("/odcsapi/tsgrouprefs", () =>
    HttpResponse.json<ApiTsGroupRef[]>(mockGroupRefs),
  ),
  computationRefs: computationHandlers.computationRefs,
  computation: computationHandlers.computation,
  saveComputation: computationHandlers.saveComputation,
  deleteComputation: computationHandlers.deleteComputation,
  algorithm: computationHandlers.algorithm,
};

export const LoadsRefsForOrganization: Story = {
  args: {},
  parameters: {
    msw: { handlers: orgScopedHandlers },
  },
  render: () => (
    <WithOrg>
      <Computations />
    </WithOrg>
  ),
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText("DailyFlowAve")).toBeInTheDocument();
  },
};

export const AppRefsFetchFailureFallsBackToEmpty: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: {
        ...orgScopedHandlers,
        appRefs: http.get(
          "/odcsapi/apprefs",
          () => new HttpResponse(null, { status: 500 }),
        ),
      },
    },
  },
  render: () => (
    <WithOrg>
      <Computations />
    </WithOrg>
  ),
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText("DailyFlowAve")).toBeInTheDocument();
  },
};

export const GroupRefsFetchFailureFallsBackToEmpty: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: {
        ...orgScopedHandlers,
        groupRefs: http.get(
          "/odcsapi/tsgrouprefs",
          () => new HttpResponse(null, { status: 500 }),
        ),
      },
    },
  },
  render: () => (
    <WithOrg>
      <Computations />
    </WithOrg>
  ),
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText("DailyFlowAve")).toBeInTheDocument();
  },
};

export const AddSaveNewComputation: Story = {
  args: {},
  parameters: {
    msw: { handlers: orgScopedHandlers },
  },
  render: () => (
    <WithOrg>
      <Computations />
    </WithOrg>
  ),
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    await canvas.findByText("DailyFlowAve");

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:add_computation"),
    });
    await act(async () => userEvent.click(addBtn));

    const nameInput = await canvas.findByRole(
      "textbox",
      { name: i18n.t("computations:editor.name") },
      { timeout: 5000 },
    );
    await userEvent.type(nameInput, "FreshComputation");

    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.save_for", { id: -1 }),
    });
    await act(async () => userEvent.click(saveBtn));

    await waitFor(() => expect(canvas.queryByText("-1")).not.toBeInTheDocument());
  },
};

const statefulDeleteHandlers = (() => {
  let remainingRefs: ApiComputationRef[] = [...mockComputationRefs];
  return {
    ...orgScopedHandlers,
    computationRefs: http.get("/odcsapi/computationrefs", () =>
      HttpResponse.json<ApiComputationRef[]>(remainingRefs),
    ),
    deleteComputation: http.delete("/odcsapi/computation", ({ request }) => {
      const url = new URL(request.url);
      const id = parseInt(url.searchParams.get("computationid") ?? "-1");
      remainingRefs = remainingRefs.filter((ref) => ref.computationId !== id);
      return new HttpResponse(null, { status: 204 });
    }),
  };
})();

export const DeleteSuccess: Story = {
  args: {},
  parameters: {
    msw: { handlers: statefulDeleteHandlers },
  },
  render: () => (
    <WithOrg>
      <Computations />
    </WithOrg>
  ),
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const deleteBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.delete_for", { id: 2 }),
    });
    await act(async () => userEvent.click(deleteBtn));

    await waitFor(() =>
      expect(canvas.queryByText("DailyStageMax")).not.toBeInTheDocument(),
    );
  },
};

export const DeleteFailureLogsError: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: {
        ...orgScopedHandlers,
        deleteComputation: http.delete(
          "/odcsapi/computation",
          () => new HttpResponse(null, { status: 500 }),
        ),
      },
    },
  },
  render: () => (
    <WithOrg>
      <Computations />
    </WithOrg>
  ),
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const deleteBtn = await canvas.findByRole("button", {
      name: i18n.t("computations:editor.delete_for", { id: 2 }),
    });
    await act(async () => userEvent.click(deleteBtn));

    // Row remains because delete failed; error is logged.
    await waitFor(() =>
      expect(canvas.queryByText("DailyStageMax")).toBeInTheDocument(),
    );
  },
};
