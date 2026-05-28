import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { http, HttpResponse } from "msw";
import type {
  ApiDataSourceRef,
  ApiNetlistRef,
  ApiPlatformRef,
  ApiPresentationRef,
  ApiRouting,
  ApiRoutingRef,
} from "opendcs-api";
import { expect, screen, waitFor } from "storybook/test";
import { RoutingsPage } from "./RoutingsPage";

const ROUTING_REFS: ApiRoutingRef[] = [
  {
    routingId: 8,
    name: "ACIS-WebService",
    dataSourceName: "karl-test-xml",
    destination: "pipe(stdout)",
    lastModified: new Date("2026-05-28T18:08:19.083Z"),
  },
  {
    routingId: 9,
    name: "GOES-Hourly",
    dataSourceName: "lrgs-main",
    destination: "file(/data/out.txt)",
    lastModified: new Date("2026-05-20T12:00:00.000Z"),
  },
];

const FULL_ROUTINGS: Record<number, ApiRouting> = {
  8: {
    routingId: 8,
    name: "ACIS-WebService",
    dataSourceName: "karl-test-xml",
    destinationType: "pipe",
    destinationArg: "stdout",
    outputFormat: "html-report",
    outputTZ: "GMT",
    presGroupName: "SHEF-English",
    applyTimeTo: "Local Receive Time",
    since: "now - 1 hour",
    until: "now",
    enableEquations: false,
    production: false,
    goesSelfTimed: false,
    goesRandom: false,
    networkDCP: false,
    iridium: false,
    qualityNotifications: false,
    goesSpacecraftCheck: false,
    goesSpacecraftSelection: "East",
    parityCheck: false,
    paritySelection: "Good",
    ascendingTime: false,
    settlingTimeDelay: false,
    platformIds: [],
    platformNames: ["Alpha"],
    netlistNames: ["ACIS-WebService"],
    goesChannels: [],
    properties: { sitenametype: "acis" },
  },
  // A richer record: most flags on, a calendar "since", a now-minus "until",
  // and a platform attached by DCP address (resolved via Bravo's transportMedia).
  9: {
    routingId: 9,
    name: "GOES-Hourly",
    dataSourceName: "lrgs-main",
    destinationType: "file",
    destinationArg: "/data/out.txt",
    outputFormat: "shef",
    outputTZ: "America/New_York",
    presGroupName: "CWMS-English",
    applyTimeTo: "Platform Xmit Time",
    since: "2024/032 06:15:00",
    until: "now - 2 hours",
    enableEquations: true,
    production: true,
    goesSelfTimed: true,
    goesRandom: false,
    networkDCP: true,
    iridium: false,
    qualityNotifications: true,
    goesSpacecraftCheck: true,
    goesSpacecraftSelection: "West",
    parityCheck: true,
    paritySelection: "Bad",
    ascendingTime: true,
    settlingTimeDelay: true,
    platformIds: ["CE2D1234"],
    platformNames: [],
    netlistNames: ["GOES-East"],
    goesChannels: [],
    properties: {},
  },
};

const PLATFORM_REFS: ApiPlatformRef[] = [
  { platformId: 1, name: "Alpha", agency: "USGS", config: "Standard CFG" },
  {
    platformId: 2,
    name: "Bravo",
    agency: "NOAA",
    config: "Backup CFG",
    transportMedia: { GOES: "CE2D1234" },
  },
];

const NETLIST_REFS: ApiNetlistRef[] = [
  {
    netlistId: 1,
    name: "ACIS-WebService",
    transportMediumType: "other",
    numPlatforms: 3,
  },
  {
    netlistId: 2,
    name: "GOES-East",
    transportMediumType: "goes",
    numPlatforms: 12,
  },
];

const DATA_SOURCE_REFS: ApiDataSourceRef[] = [
  { dataSourceId: 12, name: "karl-test-xml", type: "abstractweb" },
  { dataSourceId: 13, name: "lrgs-main", type: "lrgs" },
];

const PRESENTATION_REFS: ApiPresentationRef[] = [
  { groupId: 1, name: "SHEF-English" },
  { groupId: 2, name: "CWMS-English" },
];

const baseHandlers = {
  routingRefs: http.get("/odcsapi/routingrefs", () =>
    HttpResponse.json<ApiRoutingRef[]>(ROUTING_REFS),
  ),
  routing: http.get("/odcsapi/routing", ({ request }) => {
    const url = new URL(request.url);
    const id = Number(url.searchParams.get("routingid"));
    return HttpResponse.json<ApiRouting>(FULL_ROUTINGS[id] ?? { routingId: id });
  }),
  postRouting: http.post("/odcsapi/routing", async () =>
    HttpResponse.json<ApiRouting>({}),
  ),
  deleteRouting: http.delete("/odcsapi/routing", () => HttpResponse.json({})),
  platformRefs: http.get("/odcsapi/platformrefs", () =>
    HttpResponse.json<ApiPlatformRef[]>(PLATFORM_REFS),
  ),
  netlistRefs: http.get("/odcsapi/netlistrefs", () =>
    HttpResponse.json<ApiNetlistRef[]>(NETLIST_REFS),
  ),
  dataSourceRefs: http.get("/odcsapi/datasourcerefs", () =>
    HttpResponse.json<ApiDataSourceRef[]>(DATA_SOURCE_REFS),
  ),
  presentationRefs: http.get("/odcsapi/presentationrefs", () =>
    HttpResponse.json<ApiPresentationRef[]>(PRESENTATION_REFS),
  ),
};

const meta = {
  component: RoutingsPage,
} satisfies Meta<typeof RoutingsPage>;

export default meta;

type Story = StoryObj<typeof meta>;

// Default render: list comes back, both routing specs show.
export const Default: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText("ACIS-WebService")).toBeInTheDocument();
    expect(await canvas.findByText("GOES-Hourly")).toBeInTheDocument();
  },
};

// Consumer column shows type(arg); date column renders a real date.
export const ConsumerAndDateColumns: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText("pipe(stdout)")).toBeInTheDocument();
    expect(await canvas.findByText("file(/data/out.txt)")).toBeInTheDocument();
  },
};

// Empty state: API returns nothing — caption still renders.
export const Empty: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        routingRefs: http.get("/odcsapi/routingrefs", () =>
          HttpResponse.json<ApiRoutingRef[]>([]),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(await canvas.findByText(i18n.t("routing:title"))).toBeInTheDocument();
  },
};

// Open a routing row — detail loads, name prefilled, the since builder resolves
// "now - 1 hour" (not the buggy "null"), and the attached netlist shows.
export const OpenRoutingDetail: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const row = await canvas.findByText("ACIS-WebService");
    await act(async () => userEvent.click(row));
    await waitFor(async () => {
      const nameInput = (await canvas.findByLabelText(
        i18n.t("routing:name"),
      )) as HTMLInputElement;
      expect(nameInput.value).toEqual("ACIS-WebService");
    });
    // since amount resolved from "now - 1 hour"
    await waitFor(() => {
      const amount = canvas.getByLabelText(
        i18n.t("routing:now_minus_amount"),
      ) as HTMLInputElement;
      expect(amount.value).toEqual("1 hour");
    });
    // name-selected platform ("Alpha" from platformNames) shows in the table
    await waitFor(() => {
      expect(canvas.getByText("Alpha")).toBeInTheDocument();
    });
  },
};

// Dropdowns are prefilled from the record (data source, output format, pres group).
export const PrefilledDropdowns: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await act(async () => userEvent.click(await canvas.findByText("ACIS-WebService")));
    await waitFor(() => {
      const dataSource = canvas.getByRole("combobox", {
        name: i18n.t("routing:data_source"),
      }) as HTMLSelectElement;
      expect(dataSource.value).toEqual("karl-test-xml");
    });
    expect(
      (
        canvas.getByRole("combobox", {
          name: i18n.t("routing:output_format"),
        }) as HTMLSelectElement
      ).value,
    ).toEqual("html-report");
    expect(
      (
        canvas.getByRole("combobox", {
          name: i18n.t("routing:pres_group"),
        }) as HTMLSelectElement
      ).value,
    ).toEqual("SHEF-English");
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
      name: i18n.t("routing:edit_routing", { id: 8 }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(() => {
      expect(
        canvas.getByRole("button", {
          name: i18n.t("routing:save_routing", { id: 8 }),
        }),
      ).toBeInTheDocument();
    });
    const nameInput = canvas.getByLabelText(i18n.t("routing:name")) as HTMLInputElement;
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
      name: i18n.t("routing:edit_routing", { id: 8 }),
    });
    await act(async () => userEvent.click(editBtn));
    const cancelBtn = await canvas.findByRole("button", {
      name: i18n.t("routing:cancel_for", { id: 8 }),
    });
    await act(async () => userEvent.click(cancelBtn));
    await waitFor(() => {
      expect(
        canvas.queryByRole("button", {
          name: i18n.t("routing:cancel_for", { id: 8 }),
        }),
      ).not.toBeInTheDocument();
    });
  },
};

// The full record (id 9) reflects its flags, dependent dropdowns, and the
// calendar/now-minus time methods.
export const FullRecordReflectsState: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await act(async () => userEvent.click(await canvas.findByText("GOES-Hourly")));

    await waitFor(() => {
      expect(
        (
          canvas.getByRole("checkbox", {
            name: i18n.t("routing:production"),
          }) as HTMLInputElement
        ).checked,
      ).toBe(true);
    });
    // GOES spacecraft check on, selection West (checkbox + select share the name,
    // disambiguated by role).
    expect(
      (
        canvas.getByRole("checkbox", {
          name: i18n.t("routing:spacecraft"),
        }) as HTMLInputElement
      ).checked,
    ).toBe(true);
    expect(
      (
        canvas.getByRole("combobox", {
          name: i18n.t("routing:spacecraft"),
        }) as HTMLSelectElement
      ).value,
    ).toEqual("West");
    expect(
      (
        canvas.getByRole("combobox", {
          name: i18n.t("routing:parity"),
        }) as HTMLSelectElement
      ).value,
    ).toEqual("Bad");
    // since uses the Calendar method, until uses Now minus.
    expect(
      (
        canvas.getByRole("combobox", {
          name: i18n.t("routing:since"),
        }) as HTMLSelectElement
      ).value,
    ).toEqual("calendar");
    expect(
      (
        canvas.getByRole("combobox", {
          name: i18n.t("routing:until"),
        }) as HTMLSelectElement
      ).value,
    ).toEqual("nowMinus");
    // Address-selected platform resolves to Bravo via its transportMedia.
    await waitFor(() => expect(canvas.getByText("Bravo")).toBeInTheDocument());
    // Attached DB netlist shows in the network-list table.
    expect(canvas.getByText("GOES-East")).toBeInTheDocument();
  },
};

// The "+" header button appends a new editable routing row.
export const AddNewRoutingRow: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("routing:add_routing"),
    });
    await act(async () => userEvent.click(addBtn));
    await waitFor(() => {
      const nameInput = canvas.getByLabelText(
        i18n.t("routing:name"),
      ) as HTMLInputElement;
      expect(nameInput.value).toEqual("");
      expect(nameInput.readOnly).toBe(false);
    });
  },
};

// Deleting a routing fires the DELETE and the row drops out after the refetch.
export const DeleteRoutingRow: Story = {
  parameters: {
    msw: {
      handlers: (() => {
        const list = [...ROUTING_REFS];
        return {
          ...baseHandlers,
          routingRefs: http.get("/odcsapi/routingrefs", () =>
            HttpResponse.json<ApiRoutingRef[]>(list),
          ),
          deleteRouting: http.delete("/odcsapi/routing", ({ request }) => {
            const url = new URL(request.url);
            const id = Number(url.searchParams.get("routingid"));
            const idx = list.findIndex((r) => r.routingId === id);
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
    expect(await canvas.findByText("GOES-Hourly")).toBeInTheDocument();
    const deleteBtn = await canvas.findByRole("button", {
      name: i18n.t("routing:delete_for", { id: 9 }),
    });
    await act(async () => userEvent.click(deleteBtn));
    await waitFor(() =>
      expect(canvas.queryByText("GOES-Hourly")).not.toBeInTheDocument(),
    );
  },
};

// Edit a routing, open the platforms add-modal, pick a platform, and confirm it
// lands in the table. Exercises the shared MultiSelectorModal.
export const AddPlatformViaModal: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("routing:edit_routing", { id: 8 }),
    });
    await act(async () => userEvent.click(editBtn));
    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("routing:add_platforms"),
    });
    await act(async () => userEvent.click(addBtn));
    // Modal portals to document.body — query via screen. Alpha is already
    // attached, so Bravo is the only available row.
    const bravo = await screen.findByText("Bravo");
    await act(async () => userEvent.click(bravo));
    const confirm = await screen.findByRole("button", {
      name: i18n.t("routing:add_selected", { count: 1 }),
    });
    await act(async () => userEvent.click(confirm));
    await waitFor(() => expect(canvas.getByText("Bravo")).toBeInTheDocument());
  },
};

// Edit a routing, open the network-list add-modal, pick a netlist, and confirm.
export const AddNetlistViaModal: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("routing:edit_routing", { id: 8 }),
    });
    await act(async () => userEvent.click(editBtn));
    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("routing:add_netlists"),
    });
    await act(async () => userEvent.click(addBtn));
    // ACIS-WebService is already attached, so GOES-East is the only option.
    const goesEast = await screen.findByText("GOES-East");
    await act(async () => userEvent.click(goesEast));
    const confirm = await screen.findByRole("button", {
      name: i18n.t("routing:add_selected_netlists", { count: 1 }),
    });
    await act(async () => userEvent.click(confirm));
    await waitFor(() => expect(canvas.getByText("GOES-East")).toBeInTheDocument());
  },
};

// Edit a routing and remove its name-attached platform via the row trash action.
export const RemovePlatformRow: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("routing:edit_routing", { id: 8 }),
    });
    await act(async () => userEvent.click(editBtn));
    // "Alpha" is attached by name and shows in the platforms table.
    expect(await canvas.findByText("Alpha")).toBeInTheDocument();
    const removeBtn = await canvas.findByRole("button", {
      name: i18n.t("routing:remove_platform", { name: "Alpha" }),
    });
    await act(async () => userEvent.click(removeBtn));
    await waitFor(() => expect(canvas.queryByText("Alpha")).not.toBeInTheDocument());
  },
};
