import type { Meta, StoryObj } from "@storybook/react-vite";
import { Algorithms } from "./index";
import { http, HttpResponse } from "msw";
import type { ApiAlgorithm, ApiAlgorithmRef, ApiPropSpec } from "opendcs-api";
import { act } from "react";
import { expect, waitFor } from "storybook/test";

const meta = {
  component: Algorithms,
} satisfies Meta<typeof Algorithms>;

export default meta;

type Story = StoryObj<typeof meta>;

// Raw algorithm objects — parms stored as plain array (server shape)
const mockAlgorithms: (ApiAlgorithm & { parms?: unknown[] })[] = [
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

const mockAlgoRefs: ApiAlgorithmRef[] = mockAlgorithms.map(
  ({ algorithmId, name, execClass, description }) => ({
    algorithmId,
    algorithmName: name,
    execClass,
    description,
    numCompsUsing: 0,
  }),
);

const mockPropSpecs: ApiPropSpec[] = [
  { name: "input_fu", description: "Input engineering units", type: "String" },
  { name: "output_fu", description: "Output engineering units", type: "String" },
];

const algorithmHandlers = {
  algorithmRefs: http.get("/odcsapi/algorithmrefs", () => {
    return HttpResponse.json<ApiAlgorithmRef[]>(mockAlgoRefs);
  }),
  algorithm: http.get("/odcsapi/algorithm", ({ request }) => {
    const url = new URL(request.url);
    const id = parseInt(url.searchParams.get("algorithmid") ?? "-1");
    const algo = mockAlgorithms.find((a) => a.algorithmId === id);
    if (!algo) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json(algo);
  }),
  saveAlgorithm: http.post("/odcsapi/algorithm", async ({ request }) => {
    const body = (await request.json()) as ApiAlgorithm;
    return HttpResponse.json({ ...body, algorithmId: body.algorithmId ?? 99 });
  }),
  deleteAlgorithm: http.delete("/odcsapi/algorithm", () => {
    return new HttpResponse(null, { status: 204 });
  }),
  propSpecs: http.get("/odcsapi/propspecs", () => {
    return HttpResponse.json<ApiPropSpec[]>(mockPropSpecs);
  }),
};

export const Default: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: algorithmHandlers,
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    // Wait for algorithm list to load
    const copyAlgoCell = await canvas.findByText("CopyAlgorithm");
    expect(copyAlgoCell).toBeInTheDocument();

    // Click a row to expand it
    await act(async () => copyAlgoCell.click());

    // The editor opens in view mode — exec class field should be present
    const execClassInput = await canvas.findByRole(
      "textbox",
      { name: i18n.t("algorithms:editor.execClass") },
      { timeout: 5000 },
    );
    expect((execClassInput as HTMLInputElement).readOnly).toBeTruthy();
    expect(execClassInput).toHaveValue("decodes.comp.CopyAlgorithm");
  },
};

export const EditAndSave: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: algorithmHandlers,
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    // Wait for list to load then click Edit on algorithm 1
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("algorithms:editor.edit_for", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));

    const saveBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("algorithms:editor.save_for", { id: 1 }) },
      { timeout: 5000 },
    );
    expect(saveBtn).toBeInTheDocument();

    await act(async () => userEvent.click(saveBtn));

    // After save the edit button reappears
    await waitFor(async () => {
      const editBtnAfter = await canvas.findByRole("button", {
        name: i18n.t("algorithms:editor.edit_for", { id: 1 }),
      });
      expect(editBtnAfter).toBeInTheDocument();
    });
  },
};

export const AddAndCancel: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: algorithmHandlers,
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    // Wait for algorithm refs to finish loading before clicking "+"
    // (if we click before the fetch completes, the subsequent draw() from the fetch
    // closing the new row's child row before it can be found)
    await canvas.findByText("CopyAlgorithm");

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("algorithms:add_algorithm"),
    });
    await act(async () => userEvent.click(addBtn));

    // A new row with id -1 appears
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
