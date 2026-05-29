import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { http, HttpResponse } from "msw";
import type { ApiDataSource, ApiDataSourceRef } from "opendcs-api";
import { expect, screen, waitFor } from "storybook/test";
import { DataSourcesPage } from "./DataSourcesPage";

const DATA_SOURCE_REFS: ApiDataSourceRef[] = [
  {
    dataSourceId: 12,
    name: "karl-test-xml",
    type: "abstractweb",
    arguments: "url=https://example.com",
    usedBy: 1,
  },
  {
    dataSourceId: 13,
    name: "lrgs-main",
    type: "lrgs",
    arguments: "host=lrgs.example.com",
    usedBy: 3,
  },
  {
    dataSourceId: 14,
    name: "backup-group",
    type: "hotbackupgroup",
    arguments: "",
    usedBy: 0,
  },
];

const FULL_DATA_SOURCES: Record<number, ApiDataSource> = {
  12: {
    dataSourceId: 12,
    name: "karl-test-xml",
    type: "abstractweb",
    usedBy: 1,
    props: { url: "https://example.com", sitenametype: "acis" },
    groupMembers: [],
  },
  13: {
    dataSourceId: 13,
    name: "lrgs-main",
    type: "lrgs",
    usedBy: 3,
    props: { host: "lrgs.example.com", port: "16003" },
    groupMembers: [],
  },
  // A group source: holds member data sources instead of free-form props.
  14: {
    dataSourceId: 14,
    name: "backup-group",
    type: "hotbackupgroup",
    usedBy: 0,
    props: {},
    groupMembers: [{ dataSourceId: 13, dataSourceName: "lrgs-main" }],
  },
};

const baseHandlers = {
  dataSourceRefs: http.get("/odcsapi/datasourcerefs", () =>
    HttpResponse.json<ApiDataSourceRef[]>(DATA_SOURCE_REFS),
  ),
  dataSource: http.get("/odcsapi/datasource", ({ request }) => {
    const url = new URL(request.url);
    const id = Number(url.searchParams.get("datasourceid"));
    return HttpResponse.json<ApiDataSource>(
      FULL_DATA_SOURCES[id] ?? { dataSourceId: id },
    );
  }),
  postDataSource: http.post("/odcsapi/datasource", async () =>
    HttpResponse.json<ApiDataSource>({}),
  ),
  deleteDataSource: http.delete("/odcsapi/datasource", () => HttpResponse.json({})),
};

const meta = {
  component: DataSourcesPage,
} satisfies Meta<typeof DataSourcesPage>;

export default meta;

type Story = StoryObj<typeof meta>;

// Default render: list comes back, all data sources show.
export const Default: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText("karl-test-xml")).toBeInTheDocument();
    expect(await canvas.findByText("lrgs-main")).toBeInTheDocument();
    expect(await canvas.findByText("backup-group")).toBeInTheDocument();
  },
};

// Empty state: API returns nothing — caption still renders.
export const Empty: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        dataSourceRefs: http.get("/odcsapi/datasourcerefs", () =>
          HttpResponse.json<ApiDataSourceRef[]>([]),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(await canvas.findByText(i18n.t("datasources:title"))).toBeInTheDocument();
  },
};

// Open a data source row — detail loads, name prefilled, type dropdown reflects record.
export const OpenDataSourceDetail: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await act(async () => userEvent.click(await canvas.findByText("karl-test-xml")));
    await waitFor(async () => {
      const nameInput = (await canvas.findByLabelText(
        i18n.t("datasources:name"),
      )) as HTMLInputElement;
      expect(nameInput.value).toEqual("karl-test-xml");
    });
    await waitFor(() => {
      const type = canvas.getByRole("combobox", {
        name: i18n.t("datasources:type"),
      }) as HTMLSelectElement;
      expect(type.value).toEqual("abstractweb");
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
      name: i18n.t("datasources:edit_datasource", { id: 12 }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(() => {
      expect(
        canvas.getByRole("button", {
          name: i18n.t("datasources:save_datasource", { id: 12 }),
        }),
      ).toBeInTheDocument();
    });
    const nameInput = canvas.getByLabelText(
      i18n.t("datasources:name"),
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
      name: i18n.t("datasources:edit_datasource", { id: 12 }),
    });
    await act(async () => userEvent.click(editBtn));
    const cancelBtn = await canvas.findByRole("button", {
      name: i18n.t("datasources:cancel_for", { id: 12 }),
    });
    await act(async () => userEvent.click(cancelBtn));
    await waitFor(() => {
      expect(
        canvas.queryByRole("button", {
          name: i18n.t("datasources:cancel_for", { id: 12 }),
        }),
      ).not.toBeInTheDocument();
    });
  },
};

// The "+" header button appends a new editable data source row.
export const AddNewDataSourceRow: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("datasources:add_datasource"),
    });
    await act(async () => userEvent.click(addBtn));
    await waitFor(() => {
      const nameInput = canvas.getByLabelText(
        i18n.t("datasources:name"),
      ) as HTMLInputElement;
      expect(nameInput.value).toEqual("");
      expect(nameInput.readOnly).toBe(false);
    });
  },
};

// Deleting a data source fires the DELETE and the row drops out after the refetch.
export const DeleteDataSourceRow: Story = {
  parameters: {
    msw: {
      handlers: (() => {
        const list = [...DATA_SOURCE_REFS];
        return {
          ...baseHandlers,
          dataSourceRefs: http.get("/odcsapi/datasourcerefs", () =>
            HttpResponse.json<ApiDataSourceRef[]>(list),
          ),
          deleteDataSource: http.delete("/odcsapi/datasource", ({ request }) => {
            const url = new URL(request.url);
            const id = Number(url.searchParams.get("datasourceid"));
            const idx = list.findIndex((d) => d.dataSourceId === id);
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
    expect(await canvas.findByText("lrgs-main")).toBeInTheDocument();
    const deleteBtn = await canvas.findByRole("button", {
      name: i18n.t("datasources:delete_for", { id: 13 }),
    });
    await act(async () => userEvent.click(deleteBtn));
    await waitFor(() =>
      expect(canvas.queryByText("lrgs-main")).not.toBeInTheDocument(),
    );
  },
};

// A group-type data source reveals the group-members table with its member rows.
export const GroupTypeShowsMembers: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await act(async () => userEvent.click(await canvas.findByText("backup-group")));
    // The group-members table caption and the attached member both show.
    await waitFor(() =>
      expect(canvas.getByText(i18n.t("datasources:group_members"))).toBeInTheDocument(),
    );
    expect(await canvas.findByText("lrgs-main")).toBeInTheDocument();
  },
};

// Edit a group source, open the members add-modal, pick a member, and confirm it
// lands in the table. Exercises the shared MultiSelectorModal.
export const AddMemberViaModal: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("datasources:edit_datasource", { id: 14 }),
    });
    await act(async () => userEvent.click(editBtn));
    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("datasources:add_members"),
    });
    await act(async () => userEvent.click(addBtn));
    // lrgs-main is already a member and backup-group is itself, so karl-test-xml
    // is the only available row.
    const member = await screen.findByText("karl-test-xml");
    await act(async () => userEvent.click(member));
    const confirm = await screen.findByRole("button", {
      name: i18n.t("datasources:add_selected", { count: 1 }),
    });
    await act(async () => userEvent.click(confirm));
    await waitFor(() => expect(canvas.getByText("karl-test-xml")).toBeInTheDocument());
  },
};
