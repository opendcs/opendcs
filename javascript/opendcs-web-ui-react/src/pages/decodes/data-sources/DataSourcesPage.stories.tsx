import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { http, HttpResponse } from "msw";
import type { ApiDataSource, ApiDataSourceRef, ApiPropSpec } from "opendcs-api";
import { expect, screen, waitFor, within } from "storybook/test";
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

// What /propspecs reports for the classes behind the mocked DataSourceType list.
// Types without an entry declare no properties.
const PROP_SPECS: Record<string, ApiPropSpec[]> = {
  "decodes.datasource.LrgsDataSource": [
    { name: "host", type: "h", description: "Host name or IP Address of LRGS Server" },
    { name: "port", type: "i", description: "Listening port on LRGS Server" },
    { name: "username", type: "s", description: "DDS User name" },
  ],
  "decodes.datasource.FileDataSource": [
    { name: "filename", type: "f", description: "Name of the file to read" },
  ],
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
  propSpecs: http.get("/odcsapi/propspecs", ({ request }) => {
    const execClass = new URL(request.url).searchParams.get("class") ?? "";
    return HttpResponse.json<ApiPropSpec[]>(PROP_SPECS[execClass] ?? []);
  }),
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

// --- Add-new helpers (issue #2202) ------------------------------------------

type PlayContext = Parameters<NonNullable<Story["play"]>>[0];
type Canvas = Awaited<ReturnType<PlayContext["mount"]>>;
// Storybook needs `mount` destructured in each play's own arguments, so the
// helpers take the rest of the context.
type HelperContext = Pick<PlayContext, "userEvent" | "parameters">;

// Opens a new data source row via "+". The button's name is translated inside
// waitFor: a story that holds the list request open gets here before the
// namespace has loaded, when i18n.t still hands back the bare key.
const openNewDataSource = async (
  canvas: Canvas,
  { userEvent, parameters: { i18n } }: HelperContext,
) => {
  const addBtn = await waitFor(() =>
    canvas.getByRole("button", { name: i18n.t("datasources:add_datasource") }),
  );
  await act(async () => userEvent.click(addBtn));
};

const typeName = async (
  canvas: Canvas,
  { userEvent, parameters: { i18n } }: HelperContext,
  name: string,
): Promise<HTMLInputElement> => {
  const nameInput = (await canvas.findByRole("textbox", {
    name: i18n.t("datasources:name"),
  })) as HTMLInputElement;
  await act(async () => userEvent.type(nameInput, name));
  return nameInput;
};

const chooseType = async (
  canvas: Canvas,
  { userEvent, parameters: { i18n } }: HelperContext,
  type: string,
) => {
  const typeSelect = await canvas.findByRole("combobox", {
    name: i18n.t("datasources:type"),
  });
  await act(async () => userEvent.selectOptions(typeSelect, type));
};

const clickSave = async (
  canvas: Canvas,
  { userEvent, parameters: { i18n } }: HelperContext,
) => {
  const saveBtn = await canvas.findByRole("button", {
    name: new RegExp(`^${i18n.t("datasources:save_datasource", { id: "" }).trim()}`),
  });
  await act(async () => userEvent.click(saveBtn));
};

// The headline case from issue #2202: name + type, Save, and the new source
// lands in the list.
const savedList: ApiDataSourceRef[] = [];
const savedPosts: ApiDataSource[] = [];
export const AddNewDataSourceSaves: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        dataSourceRefs: http.get("/odcsapi/datasourcerefs", () =>
          HttpResponse.json<ApiDataSourceRef[]>(savedList),
        ),
        postDataSource: http.post("/odcsapi/datasource", async ({ request }) => {
          const body = (await request.json()) as ApiDataSource;
          savedPosts.push(body);
          savedList.push({ dataSourceId: 15, name: body.name, type: body.type });
          return HttpResponse.json<ApiDataSource>(
            { ...body, dataSourceId: 15 },
            { status: 201 },
          );
        }),
      },
    },
  },
  play: async ({ mount, ...ctx }) => {
    savedList.splice(0, savedList.length, ...DATA_SOURCE_REFS);
    savedPosts.length = 0;
    const canvas = await mount();
    await openNewDataSource(canvas, ctx);
    await typeName(canvas, ctx, "New-Source");
    await chooseType(canvas, ctx, "lrgs");
    await clickSave(canvas, ctx);
    expect(await canvas.findByText("New-Source")).toBeInTheDocument();
    expect(savedPosts).toHaveLength(1);
    expect(savedPosts[0]).toMatchObject({ name: "New-Source", type: "lrgs" });
    // The row's placeholder id must stay out of the body: the API reads an id
    // as "overwrite that record".
    expect(savedPosts[0]).not.toHaveProperty("dataSourceId");
  },
};

// Saving a new data source without choosing a type says which fields are
// required instead of posting and silently failing. The type select has to
// start blank: without a blank option it displayed its first entry while the
// record held no type at all (issue #2202).
const requiredFieldsPosts: unknown[] = [];
export const AddNewDataSourceRequiresFields: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        postDataSource: http.post("/odcsapi/datasource", async ({ request }) => {
          requiredFieldsPosts.push(await request.json());
          return HttpResponse.json<ApiDataSource>({});
        }),
      },
    },
  },
  play: async ({ mount, ...ctx }) => {
    requiredFieldsPosts.length = 0;
    const canvas = await mount();
    const { i18n } = ctx.parameters;
    await openNewDataSource(canvas, ctx);
    await typeName(canvas, ctx, "New-Source");
    const typeSelect = canvas.getByRole("combobox", {
      name: i18n.t("datasources:type"),
    }) as HTMLSelectElement;
    expect(typeSelect.value).toEqual("");
    await clickSave(canvas, ctx);
    expect(
      await canvas.findByText(i18n.t("datasources:required_fields")),
    ).toBeInTheDocument();
    expect(requiredFieldsPosts).toHaveLength(0);
    // Once a type is chosen the same Save goes through and the message clears.
    await chooseType(canvas, ctx, "lrgs");
    await clickSave(canvas, ctx);
    await waitFor(() =>
      expect(requiredFieldsPosts).toEqual([{ name: "New-Source", type: "lrgs" }]),
    );
    await waitFor(() =>
      expect(
        canvas.queryByText(i18n.t("datasources:required_fields")),
      ).not.toBeInTheDocument(),
    );
  },
};

// A server rejection on a new data source shows the server's message and keeps
// the row open with the user's input, for both error codes the endpoint
// documents (issue #2202).
const SAVE_FAILURES = [
  { status: 400, message: "Data source name is required." },
  { status: 500, message: "Error writing data source" },
];
let saveFailureCount = 0;
export const AddNewDataSourceShowsSaveError: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        postDataSource: http.post("/odcsapi/datasource", () => {
          const { status, message } = SAVE_FAILURES[saveFailureCount++];
          return HttpResponse.json({ message }, { status });
        }),
      },
    },
  },
  play: async ({ mount, ...ctx }) => {
    saveFailureCount = 0;
    const canvas = await mount();
    await openNewDataSource(canvas, ctx);
    const nameInput = await typeName(canvas, ctx, "New-Source");
    await chooseType(canvas, ctx, "lrgs");
    for (const { message } of SAVE_FAILURES) {
      await clickSave(canvas, ctx);
      expect(await canvas.findByText(message)).toBeInTheDocument();
      expect(nameInput.value).toEqual("New-Source");
    }
  },
};

// --- Properties offered by the type ------------------------------------------

const editPropButton = ({ parameters: { i18n } }: HelperContext, name: string) => ({
  name: i18n.t("properties:edit_prop", { name }),
});

// Edits one row of the properties table and saves that row. An empty value
// saves the row as it is.
const setProperty = async (
  canvas: Canvas,
  ctx: HelperContext,
  name: string,
  value: string,
) => {
  const {
    userEvent,
    parameters: { i18n },
  } = ctx;
  const editBtn = await canvas.findByRole("button", editPropButton(ctx, name));
  await act(async () => userEvent.click(editBtn));
  const valueInput = await canvas.findByRole("textbox", {
    name: i18n.t("properties:value_input", { name }),
  });
  if (value) await act(async () => userEvent.type(valueInput, value));
  const savePropName = { name: i18n.t("properties:save_prop", { name }) };
  const saveBtn = await canvas.findByRole("button", savePropName);
  await act(async () => userEvent.click(saveBtn));
  // The table redraws once the row is saved; wait so the next click lands on
  // the redrawn row.
  await waitFor(() =>
    expect(canvas.queryByRole("button", savePropName)).not.toBeInTheDocument(),
  );
};

// Choosing a type lists the properties that type accepts, and the list follows
// the type when it changes. Before a type is chosen there is nothing to offer.
export const TypeOffersItsProperties: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, ...ctx }) => {
    const canvas = await mount();
    await openNewDataSource(canvas, ctx);
    await typeName(canvas, ctx, "New-Source");
    expect(
      canvas.queryByRole("button", editPropButton(ctx, "host")),
    ).not.toBeInTheDocument();
    await chooseType(canvas, ctx, "lrgs");
    for (const name of ["host", "port", "username"]) {
      expect(
        await canvas.findByRole("button", editPropButton(ctx, name)),
      ).toBeInTheDocument();
    }
    // Each offered name carries the type's own description of it.
    expect(canvas.getByTitle("Listening port on LRGS Server")).toHaveTextContent(
      "port",
    );
    await chooseType(canvas, ctx, "file");
    expect(
      await canvas.findByRole("button", editPropButton(ctx, "filename")),
    ).toBeInTheDocument();
    expect(
      canvas.queryByRole("button", editPropButton(ctx, "host")),
    ).not.toBeInTheDocument();
  },
};

// An offered property is not a value: only what the user fills in is posted,
// and a declared property saved blank stays unset instead of being posted as
// "", which the data source would read as a real (empty) setting.
const filledPropsPosts: unknown[] = [];
export const OnlyFilledPropertiesAreSaved: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        postDataSource: http.post("/odcsapi/datasource", async ({ request }) => {
          filledPropsPosts.push(await request.json());
          return HttpResponse.json<ApiDataSource>({});
        }),
      },
    },
  },
  play: async ({ mount, ...ctx }) => {
    filledPropsPosts.length = 0;
    const canvas = await mount();
    await openNewDataSource(canvas, ctx);
    await typeName(canvas, ctx, "New-Source");
    await chooseType(canvas, ctx, "lrgs");
    await setProperty(canvas, ctx, "host", "lrgs.example.com");
    await setProperty(canvas, ctx, "port", "");
    await clickSave(canvas, ctx);
    await waitFor(() =>
      expect(filledPropsPosts).toEqual([
        { name: "New-Source", type: "lrgs", props: { host: "lrgs.example.com" } },
      ]),
    );
  },
};

// A saved property fills the row its type declares even when the two differ in
// case, the way OpenDCS reads them, and saved properties the type does not
// declare are still listed.
export const SavedPropertiesFillDeclaredRows: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        dataSource: http.get("/odcsapi/datasource", () =>
          HttpResponse.json<ApiDataSource>({
            ...FULL_DATA_SOURCES[13],
            props: { HOST: "lrgs.example.com", single: "true" },
          }),
        ),
      },
    },
  },
  play: async ({ mount, ...ctx }) => {
    const canvas = await mount();
    const {
      userEvent,
      parameters: { i18n },
    } = ctx;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("datasources:edit_datasource", { id: 13 }),
    });
    await act(async () => userEvent.click(editBtn));
    for (const name of ["HOST", "port", "username", "single"]) {
      expect(
        await canvas.findByRole("button", editPropButton(ctx, name)),
      ).toBeInTheDocument();
    }
    expect(
      canvas.queryByRole("button", editPropButton(ctx, "host")),
    ).not.toBeInTheDocument();
    expect(canvas.getByText("lrgs.example.com")).toBeInTheDocument();
  },
};

// The properties table stays out of the form until the type's specs are in. If
// it showed the saved properties first, the specs arriving would redraw every
// row under the user: a click on a row's edit button would be swallowed, and a
// property already being edited would lose what was typed.
let releasePropSpecs: () => void = () => {};
export const PropertiesWaitForSpecs: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        propSpecs: http.get("/odcsapi/propspecs", async () => {
          await new Promise<void>((resolve) => (releasePropSpecs = resolve));
          return HttpResponse.json<ApiPropSpec[]>(
            PROP_SPECS["decodes.datasource.LrgsDataSource"],
          );
        }),
      },
    },
  },
  play: async ({ mount, ...ctx }) => {
    const canvas = await mount();
    const {
      userEvent,
      parameters: { i18n },
    } = ctx;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("datasources:edit_datasource", { id: 13 }),
    });
    await act(async () => userEvent.click(editBtn));
    // The form itself is up, with the record's own fields filled in.
    await waitFor(() =>
      expect(
        canvas.getByRole("textbox", { name: i18n.t("datasources:name") }),
      ).toHaveValue("lrgs-main"),
    );
    // The table's caption renders with the table itself, before any row does,
    // so its absence means the table is not there at all.
    expect(
      canvas.queryByText(i18n.t("properties:PropertiesTitle")),
    ).not.toBeInTheDocument();
    await act(async () => releasePropSpecs());
    expect(
      await canvas.findByText(i18n.t("properties:PropertiesTitle")),
    ).toBeInTheDocument();
    for (const name of ["host", "port", "username"]) {
      expect(
        await canvas.findByRole("button", editPropButton(ctx, name)),
      ).toBeInTheDocument();
    }
  },
};

// Property specs are a convenience. When the API cannot describe a type's class
// the form still opens and shows the properties that were saved.
let failedSpecRequests = 0;
export const PropertySpecsUnavailable: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        propSpecs: http.get("/odcsapi/propspecs", () => {
          failedSpecRequests++;
          return HttpResponse.json(
            { message: "Cannot get property specs" },
            { status: 409 },
          );
        }),
      },
    },
  },
  play: async ({ mount, ...ctx }) => {
    failedSpecRequests = 0;
    const canvas = await mount();
    const {
      userEvent,
      parameters: { i18n },
    } = ctx;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("datasources:edit_datasource", { id: 13 }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(() => expect(failedSpecRequests).toBeGreaterThan(0));
    expect(
      await canvas.findByRole("button", editPropButton(ctx, "host")),
    ).toBeInTheDocument();
    expect(
      canvas.queryByRole("button", editPropButton(ctx, "username")),
    ).not.toBeInTheDocument();
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
    const confirmBtn = await screen.findByRole("button", {
      name: i18n.t("translation:delete"),
    });
    await act(async () => userEvent.click(confirmBtn));
    await waitFor(() =>
      expect(canvas.queryByText("lrgs-main")).not.toBeInTheDocument(),
    );
  },
};

// A group-type data source reveals the group-members table with its member rows.
export const GroupTypeShowsMembers: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, canvasElement, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await act(async () => userEvent.click(await canvas.findByText("backup-group")));
    // The group-members table caption and the attached member both show.
    await waitFor(() =>
      expect(canvas.getByText(i18n.t("datasources:group_members"))).toBeInTheDocument(),
    );
    // "lrgs-main" also appears in the main list, so scope to the members table.
    const membersTable = await waitFor(() => {
      const el = canvasElement.querySelector<HTMLElement>("#dataSourceMembersTable");
      if (!el) throw new Error("members table not yet rendered");
      return el;
    });
    expect(await within(membersTable).findByText("lrgs-main")).toBeInTheDocument();
  },
};

// Edit a group source, open the members add-modal, pick a member, and confirm it
// lands in the table. Exercises the shared MultiSelectorModal.
export const AddMemberViaModal: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, canvasElement, userEvent, parameters }) => {
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
    // The modal portals to body and "karl-test-xml" also exists in the main
    // list, so scope to the dialog. lrgs-main is already a member and
    // backup-group is itself, so karl-test-xml is the only available row.
    const dialog = await screen.findByRole("dialog");
    const member = await within(dialog).findByText("karl-test-xml");
    await act(async () => userEvent.click(member));
    const confirm = await within(dialog).findByRole("button", {
      name: i18n.t("datasources:add_selected", { count: 1 }),
    });
    await act(async () => userEvent.click(confirm));
    // After confirm the member lands in the members table; scope there since the
    // name also appears in the main list.
    const membersTable = await waitFor(() => {
      const el = canvasElement.querySelector<HTMLElement>("#dataSourceMembersTable");
      if (!el) throw new Error("members table not yet rendered");
      return el;
    });
    await waitFor(() =>
      expect(within(membersTable).getByText("karl-test-xml")).toBeInTheDocument(),
    );
  },
};

// The row is opened before the data source list arrives. The detail row is
// never re-rendered with new props, so the member chooser must pick the list up
// itself rather than stay on the empty/loading snapshot it opened with
// (issue #2202).
let releaseDataSourceRefs: () => void = () => {};
export const MemberChooserLoadsAfterRowOpens: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        dataSourceRefs: http.get("/odcsapi/datasourcerefs", async () => {
          await new Promise<void>((resolve) => (releaseDataSourceRefs = resolve));
          return HttpResponse.json<ApiDataSourceRef[]>(DATA_SOURCE_REFS);
        }),
      },
    },
  },
  play: async ({ mount, ...ctx }) => {
    const canvas = await mount();
    const {
      userEvent,
      parameters: { i18n },
    } = ctx;
    await openNewDataSource(canvas, ctx);
    await chooseType(canvas, ctx, "hotbackupgroup");
    const addMembers = await canvas.findByRole("button", {
      name: i18n.t("datasources:add_members"),
    });
    await act(async () => userEvent.click(addMembers));
    const dialog = await screen.findByRole("dialog");
    expect(await within(dialog).findByRole("status")).toBeInTheDocument();
    await act(async () => releaseDataSourceRefs());
    expect(await within(dialog).findByText("karl-test-xml")).toBeInTheDocument();
  },
};
