import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { http, HttpResponse } from "msw";
import type {
  ApiDataType,
  ApiPresentationElement,
  ApiPresentationGroup,
  ApiPresentationRef,
  ApiUnit,
} from "opendcs-api";
import { expect, waitFor } from "storybook/test";
import { PresentationsPage } from "./PresentationsPage";

const PRESENTATION_REFS: ApiPresentationRef[] = [
  {
    groupId: 1,
    name: "shef-english",
    inheritsFrom: undefined,
    production: true,
    lastModified: new Date("2022-04-05T14:36:55.705Z"),
  },
  {
    groupId: 2,
    name: "shef-english-derived",
    inheritsFrom: "shef-english",
    inheritsFromId: 1,
    production: false,
    lastModified: new Date("2023-09-19T18:14:14.788Z"),
  },
];

const FULL_PRESENTATIONS: Record<number, ApiPresentationGroup> = {
  1: {
    groupId: 1,
    name: "shef-english",
    production: true,
    lastModified: new Date("2022-04-05T14:36:55.705Z"),
    elements: [
      {
        dataTypeStd: "SHEF-PE",
        dataTypeCode: "HG",
        units: "ft",
        fractionalDigits: 2,
        min: 0,
        max: 100,
      },
    ],
  },
  2: {
    groupId: 2,
    name: "shef-english-derived",
    inheritsFrom: "shef-english",
    inheritsFromId: 1,
    production: false,
    lastModified: new Date("2023-09-19T18:14:14.788Z"),
    elements: [],
  },
};

const MOCK_DATA_TYPES: ApiDataType[] = [
  { standard: "SHEF-PE", code: "HG", displayName: "Stage" },
  { standard: "SHEF-PE", code: "QR", displayName: "Flow" },
  { standard: "CWMS", code: "Stage", displayName: "Stage" },
  { standard: "EPA", code: "00060", displayName: "Discharge" },
];

const MOCK_UNITS: Record<number, ApiUnit> = {
  1: { abbr: "ft", name: "feet" },
  2: { abbr: "m", name: "meters" },
  3: { abbr: "cfs", name: "cubic feet per second" },
  4: { abbr: "raw", name: "raw" },
};

const baseHandlers = {
  presentationRefs: http.get("/odcsapi/presentationrefs", () =>
    HttpResponse.json<ApiPresentationRef[]>(PRESENTATION_REFS),
  ),
  presentation: http.get("/odcsapi/presentation", ({ request }) => {
    const url = new URL(request.url);
    const id = Number(url.searchParams.get("groupid"));
    return HttpResponse.json<ApiPresentationGroup>(
      FULL_PRESENTATIONS[id] ?? { groupId: id },
    );
  }),
  postPresentation: http.post("/odcsapi/presentation", async () =>
    HttpResponse.json<ApiPresentationGroup>({}),
  ),
  deletePresentation: http.delete("/odcsapi/presentation", () => HttpResponse.json({})),
  dataTypes: http.get("/odcsapi/datatypelist", () =>
    HttpResponse.json<ApiDataType[]>(MOCK_DATA_TYPES),
  ),
  units: http.get("/odcsapi/unitlist", () =>
    HttpResponse.json<Record<number, ApiUnit>>(MOCK_UNITS),
  ),
};

const meta = {
  component: PresentationsPage,
} satisfies Meta<typeof PresentationsPage>;

export default meta;

type Story = StoryObj<typeof meta>;

// Default render: list comes back, all presentations show. "shef-english"
// appears twice — as the parent's Name and as the derived row's "Inherits
// From" value — so assert both occurrences rather than a single match.
export const Default: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findAllByText("shef-english")).toHaveLength(2);
    expect(await canvas.findByText("shef-english-derived")).toBeInTheDocument();
  },
};

// Empty state: API returns nothing — caption still renders.
export const Empty: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        presentationRefs: http.get("/odcsapi/presentationrefs", () =>
          HttpResponse.json<ApiPresentationRef[]>([]),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(await canvas.findByText(i18n.t("presentations:title"))).toBeInTheDocument();
  },
};

// Open a presentation row — detail loads, name prefilled. The derived row is
// clicked because its name is unique on the page (the parent's name also
// appears as the derived row's "Inherits From" cell value).
export const OpenPresentationDetail: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await act(async () =>
      userEvent.click(await canvas.findByText("shef-english-derived")),
    );
    await waitFor(async () => {
      const nameInput = (await canvas.findByLabelText(
        i18n.t("presentations:name"),
      )) as HTMLInputElement;
      expect(nameInput.value).toEqual("shef-english-derived");
    });
  },
};

// Open in edit mode via the row edit-action; the Save button appears and the
// name field becomes editable.
export const EditMode: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:edit_presentation", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(() => {
      expect(
        canvas.getByRole("button", {
          name: i18n.t("presentations:save_presentation", { id: 1 }),
        }),
      ).toBeInTheDocument();
    });
    const nameInput = canvas.getByLabelText(
      i18n.t("presentations:name"),
    ) as HTMLInputElement;
    expect(nameInput.readOnly).toBe(false);
  },
};

// Open in edit mode then cancel — the row closes and the cancel button goes away.
export const EditAndCancel: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:edit_presentation", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));
    const cancelBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:cancel_for", { id: 1 }),
    });
    await act(async () => userEvent.click(cancelBtn));
    await waitFor(() => {
      expect(
        canvas.queryByRole("button", {
          name: i18n.t("presentations:cancel_for", { id: 1 }),
        }),
      ).not.toBeInTheDocument();
    });
  },
};

// The "+" header button appends a new editable presentation row.
export const AddNewPresentationRow: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:add_presentation"),
    });
    await act(async () => userEvent.click(addBtn));
    await waitFor(() => {
      const nameInput = canvas.getByLabelText(
        i18n.t("presentations:name"),
      ) as HTMLInputElement;
      expect(nameInput.value).toEqual("");
      expect(nameInput.readOnly).toBe(false);
    });
  },
};

// Deleting a presentation fires the DELETE and the row drops out after the refetch.
export const DeletePresentationRow: Story = {
  parameters: {
    msw: {
      handlers: (() => {
        const list = [...PRESENTATION_REFS];
        return {
          ...baseHandlers,
          presentationRefs: http.get("/odcsapi/presentationrefs", () =>
            HttpResponse.json<ApiPresentationRef[]>(list),
          ),
          deletePresentation: http.delete("/odcsapi/presentation", ({ request }) => {
            const url = new URL(request.url);
            const id = Number(url.searchParams.get("groupid"));
            const idx = list.findIndex((p) => p.groupId === id);
            if (idx >= 0) list.splice(idx, 1);
            return HttpResponse.json({});
          }),
        };
      })(),
    },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(await canvas.findByText("shef-english-derived")).toBeInTheDocument();
    const deleteBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:delete_for", { id: 2 }),
    });
    await act(async () => userEvent.click(deleteBtn));
    await waitFor(() =>
      expect(canvas.queryByText("shef-english-derived")).not.toBeInTheDocument(),
    );
  },
};

// Shared handler factory for stories that need to inspect the POST body.
const makeCapturingHandlers = () => {
  let lastBody: unknown = null;
  return {
    ...baseHandlers,
    postPresentation: http.post("/odcsapi/presentation", async ({ request }) => {
      lastBody = await request.json();
      return HttpResponse.json<ApiPresentationGroup>(lastBody as ApiPresentationGroup);
    }),
    getLastBody: () => lastBody,
  };
};

// The POST body must include the existing element (units="ft"), not an empty array.
export const SavePreservesExistingElement: Story = {
  parameters: {
    msw: { handlers: makeCapturingHandlers() },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n, msw } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:edit_presentation", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));
    const saveBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:save_presentation", { id: 1 }),
    });
    await act(async () => userEvent.click(saveBtn));
    await waitFor(() => {
      const body = msw.handlers.getLastBody?.() as ApiPresentationGroup | null;
      expect(body?.elements).toHaveLength(1);
      expect((body?.elements?.[0] as ApiPresentationElement)?.units).toBe("ft");
    });
  },
};

// Regression: saving a brand-new presentation always sends elements: [] (not undefined/omitted).
export const NewPresentationSendsEmptyElements: Story = {
  parameters: {
    msw: { handlers: makeCapturingHandlers() },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n, msw } = parameters;
    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:add_presentation"),
    });
    await act(async () => userEvent.click(addBtn));
    // DetailFade hides the form during its enter animation; retry until focusable.
    await waitFor(async () => {
      await userEvent.clear(
        canvas.getByLabelText(i18n.t("presentations:name")) as HTMLInputElement,
      );
    });
    await act(async () => {
      await userEvent.type(
        canvas.getByLabelText(i18n.t("presentations:name")) as HTMLInputElement,
        "new-group",
      );
    });
    // New presentation's groupId is a synthetic negative int; match by prefix.
    const saveBtn = await canvas.findByRole("button", {
      name: new RegExp(i18n.t("presentations:save_presentation", { id: "" }).trimEnd()),
    });
    await act(async () => userEvent.click(saveBtn));
    await waitFor(() => {
      const body = msw.handlers.getLastBody?.() as ApiPresentationGroup | null;
      expect(body).not.toBeNull();
      expect(Array.isArray(body?.elements)).toBe(true);
    });
  },
};

// Regression: adding an element and saving persists it in the POST body.
export const AddAndSaveElement: Story = {
  parameters: {
    msw: { handlers: makeCapturingHandlers() },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n, msw } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:edit_presentation", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));
    const addElementBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:elements.add"),
    });
    await act(async () => userEvent.click(addElementBtn));
    // New-row aria-labels include a counter suffix (e.g. "…for 1"); match by prefix.
    // dataTypeStd renders as a <select> (role "combobox"); dataTypeCode is a text input.
    const stdInput = (await canvas.findByRole("combobox", {
      name: new RegExp(
        i18n.t("presentations:elements.dataTypeStd_input", { name: "" }).trimEnd(),
      ),
    })) as HTMLSelectElement;
    const codeInput = (await canvas.findByRole("textbox", {
      name: new RegExp(
        i18n.t("presentations:elements.dataTypeCode_input", { name: "" }).trimEnd(),
      ),
    })) as HTMLInputElement;
    await act(async () => {
      await userEvent.selectOptions(stdInput, "SHEF-PE");
      await userEvent.type(codeInput, "HP");
    });
    const saveElementBtn = await canvas.findByRole("button", {
      name: new RegExp(
        i18n.t("presentations:elements.save_edit", { name: "" }).trimEnd(),
      ),
    });
    await act(async () => userEvent.click(saveElementBtn));
    await waitFor(() =>
      expect(
        canvas.queryByRole("button", {
          name: new RegExp(
            i18n.t("presentations:elements.save_edit", { name: "" }).trimEnd(),
          ),
        }),
      ).not.toBeInTheDocument(),
    );
    const savePresentationBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:save_presentation", { id: 1 }),
    });
    await act(async () => userEvent.click(savePresentationBtn));
    await waitFor(() => {
      const body = msw.handlers.getLastBody?.() as ApiPresentationGroup | null;
      expect(body?.elements).toHaveLength(2);
    });
  },
};

// Regression: adding an element with a duplicate dataTypeStd|dataTypeCode keeps
// the row in edit mode and highlights the std/code inputs with border-warning.
// The existing element's values (e.g. units="ft") must remain intact.
export const AddDuplicateElementRejected: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:edit_presentation", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));
    const addElementBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:elements.add"),
    });
    await act(async () => userEvent.click(addElementBtn));
    // New-row aria-labels include a counter suffix (e.g. "…for 1"); match by prefix.
    // dataTypeStd renders as a <select> (role "combobox"); dataTypeCode is a text input.
    const stdInput = (await canvas.findByRole("combobox", {
      name: new RegExp(
        i18n.t("presentations:elements.dataTypeStd_input", { name: "" }).trimEnd(),
      ),
    })) as HTMLSelectElement;
    const codeInput = (await canvas.findByRole("textbox", {
      name: new RegExp(
        i18n.t("presentations:elements.dataTypeCode_input", { name: "" }).trimEnd(),
      ),
    })) as HTMLInputElement;
    // Select the same std/code as the existing "HG" element.
    await act(async () => {
      await userEvent.selectOptions(stdInput, "SHEF-PE");
      await userEvent.type(codeInput, "HG");
    });
    // Save button label is counter-based ("Save element 1"), not the typed code.
    const saveElementBtn = await canvas.findByRole("button", {
      name: new RegExp(
        i18n.t("presentations:elements.save_edit", { name: "" }).trimEnd(),
      ),
    });
    await act(async () => userEvent.click(saveElementBtn));

    // The duplicate fields must carry the warning border class.
    expect(stdInput).toHaveClass("border-warning");
    expect(codeInput).toHaveClass("border-warning");

    // The save button is still present — row stays in edit mode.
    expect(
      canvas.getByRole("button", {
        name: new RegExp(
          i18n.t("presentations:elements.save_edit", { name: "" }).trimEnd(),
        ),
      }),
    ).toBeInTheDocument();

    // The existing "HG" element's units value has not been wiped.
    expect(canvas.getByText("ft", { selector: "td" })).toBeInTheDocument();
  },
};
