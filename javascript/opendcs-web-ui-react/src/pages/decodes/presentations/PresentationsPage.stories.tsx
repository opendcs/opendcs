import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { http, HttpResponse } from "msw";
import type { ApiPresentationGroup, ApiPresentationRef } from "opendcs-api";
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
