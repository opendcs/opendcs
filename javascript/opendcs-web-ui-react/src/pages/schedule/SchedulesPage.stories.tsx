import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { http, HttpResponse } from "msw";
import type {
  ApiAppRef,
  ApiRoutingRef,
  ApiScheduleEntry,
  ApiScheduleEntryRef,
} from "opendcs-api";
import { expect, screen, waitFor, within } from "storybook/test";
import { SchedulesPage } from "./SchedulesPage";

const SCHEDULE_REFS: ApiScheduleEntryRef[] = [
  {
    schedEntryId: 9,
    name: "goes1",
    appName: "RoutingScheduler",
    routingSpecName: "goes1",
    enabled: true,
    lastModified: new Date("2020-12-15T17:52:13.934Z"),
  },
  {
    schedEntryId: 10,
    name: "goes2",
    appName: "RoutingScheduler",
    routingSpecName: "goes2",
    enabled: false,
    lastModified: new Date("2020-12-15T17:53:06.043Z"),
  },
  {
    schedEntryId: 17,
    name: "no_app_assigned",
    routingSpecName: "polltest",
    enabled: false,
    lastModified: new Date("2022-03-23T13:54:09.188Z"),
  },
];

const FULL_SCHEDULES: Record<number, ApiScheduleEntry> = {
  9: {
    schedEntryId: 9,
    name: "goes1",
    appId: 1,
    appName: "RoutingScheduler",
    routingSpecId: 101,
    routingSpecName: "goes1",
    enabled: true,
    startTime: new Date("2024-01-01T08:00:00Z"),
    timeZone: "UTC",
    runInterval: "1 hour",
  },
  10: {
    schedEntryId: 10,
    name: "goes2",
    appId: 1,
    appName: "RoutingScheduler",
    routingSpecId: 102,
    routingSpecName: "goes2",
    enabled: false,
    startTime: new Date("2024-02-15T12:30:00Z"),
    timeZone: "America/New_York",
    runInterval: "",
  },
  17: {
    schedEntryId: 17,
    name: "no_app_assigned",
    routingSpecId: 103,
    routingSpecName: "polltest",
    enabled: false,
    timeZone: "UTC",
    runInterval: "",
  },
};

const APP_REFS: ApiAppRef[] = [
  { appId: 1, appName: "RoutingScheduler", appType: "routingscheduler" },
  { appId: 2, appName: "compproc", appType: "computationprocess" },
];

const ROUTING_REFS: ApiRoutingRef[] = [
  { routingId: 101, name: "goes1" },
  { routingId: 102, name: "goes2" },
  { routingId: 103, name: "polltest" },
];

const baseHandlers = {
  scheduleRefs: http.get("/odcsapi/schedulerefs", () =>
    HttpResponse.json<ApiScheduleEntryRef[]>(SCHEDULE_REFS),
  ),
  schedule: http.get("/odcsapi/schedule", ({ request }) => {
    const url = new URL(request.url);
    const id = Number(url.searchParams.get("scheduleid"));
    return HttpResponse.json<ApiScheduleEntry>(
      FULL_SCHEDULES[id] ?? { schedEntryId: id },
    );
  }),
  postSchedule: http.post("/odcsapi/schedule", async () =>
    HttpResponse.json<ApiScheduleEntry>({}),
  ),
  deleteSchedule: http.delete("/odcsapi/schedule", () => HttpResponse.json({})),
  appRefs: http.get("/odcsapi/apprefs", () => HttpResponse.json<ApiAppRef[]>(APP_REFS)),
  routingRefs: http.get("/odcsapi/routingrefs", () =>
    HttpResponse.json<ApiRoutingRef[]>(ROUTING_REFS),
  ),
};

const meta = {
  component: SchedulesPage,
} satisfies Meta<typeof SchedulesPage>;

export default meta;

type Story = StoryObj<typeof meta>;

// Default render: list comes back, all schedule entries show.
export const Default: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount }) => {
    const canvas = await mount();
    // Name and Routing Spec columns both render the entry's name when they
    // happen to match, so each appears twice — query with All*.
    expect((await canvas.findAllByText("goes1")).length).toBeGreaterThan(0);
    expect((await canvas.findAllByText("goes2")).length).toBeGreaterThan(0);
    expect(await canvas.findByText("no_app_assigned")).toBeInTheDocument();
  },
};

// Empty state: API returns nothing — caption still renders.
export const Empty: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        scheduleRefs: http.get("/odcsapi/schedulerefs", () =>
          HttpResponse.json<ApiScheduleEntryRef[]>([]),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(await canvas.findByText(i18n.t("schedule:title"))).toBeInTheDocument();
  },
};

// Open a schedule row — detail loads, name prefilled, loading-app select and
// routing-spec input both reflect the saved record. Routing spec is now a
// chooser-modal trigger, so the display is a read-only text input — not a
// <select>.
export const OpenScheduleDetail: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await act(async () => userEvent.click((await canvas.findAllByText("goes1"))[0]));
    await waitFor(() => {
      const nameInput = canvas.getByLabelText(
        i18n.t("schedule:name"),
      ) as HTMLInputElement;
      expect(nameInput.value).toEqual("goes1");
    });
    await waitFor(() => {
      const app = canvas.getByRole("combobox", {
        name: i18n.t("schedule:loading_app"),
      }) as HTMLSelectElement;
      expect(app.value).toEqual("RoutingScheduler");
    });
    await waitFor(() => {
      const routing = canvas.getByLabelText(
        i18n.t("schedule:routing_spec"),
      ) as HTMLInputElement;
      expect(routing.value).toEqual("goes1");
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
      name: i18n.t("schedule:edit_schedule", { id: 9 }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(
      () => {
        expect(
          canvas.getByRole("button", {
            name: i18n.t("schedule:save_schedule", { id: 9 }),
          }),
        ).toBeInTheDocument();
      },
      { timeout: 5000 },
    );
    const nameInput = canvas.getByLabelText(
      i18n.t("schedule:name"),
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
      name: i18n.t("schedule:edit_schedule", { id: 9 }),
    });
    await act(async () => userEvent.click(editBtn));
    // Wait for the detail's fade-in to finish (the Cancel button becoming
    // accessible confirms edit mode is interactive).
    const cancelBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("schedule:cancel_for", { id: 9 }),
        }),
      { timeout: 5000 },
    );
    await act(async () => userEvent.click(cancelBtn));
    await waitFor(() => {
      expect(
        canvas.queryByRole("button", {
          name: i18n.t("schedule:cancel_for", { id: 9 }),
        }),
      ).not.toBeInTheDocument();
    });
  },
};

// The "+" header button appends a new editable schedule row.
export const AddNewScheduleRow: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("schedule:add_schedule"),
    });
    await act(async () => userEvent.click(addBtn));
    await waitFor(
      () => {
        const nameInput = canvas.getByLabelText(
          i18n.t("schedule:name"),
        ) as HTMLInputElement;
        expect(nameInput.value).toEqual("");
        expect(nameInput.readOnly).toBe(false);
      },
      { timeout: 5000 },
    );
  },
};

// `goes1` has both startTime and a parseable runInterval, so the editor should
// derive the "Run Every" mode and populate the amount/unit inputs.
export const RunEveryModeReflected: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await act(async () => userEvent.click((await canvas.findAllByText("goes1"))[0]));
    await waitFor(() => {
      const every = canvas.getByRole("radio", {
        name: i18n.t("schedule:run_every"),
      }) as HTMLInputElement;
      expect(every.checked).toBe(true);
    });
    const amount = canvas.getByLabelText(
      i18n.t("schedule:run_amount"),
    ) as HTMLInputElement;
    expect(amount.value).toEqual("1");
    const unit = canvas.getByLabelText(
      i18n.t("schedule:run_unit"),
    ) as HTMLSelectElement;
    expect(unit.value).toEqual("hour");
  },
};

// `goes2` has a startTime but no runInterval — "Run Once" mode. The amount
// and unit selects should be disabled but the start-time picker stays
// enabled (once the user opens for edit).
export const RunOnceModeReflected: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("schedule:edit_schedule", { id: 10 }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(() => {
      const once = canvas.getByRole("radio", {
        name: i18n.t("schedule:run_once"),
      }) as HTMLInputElement;
      expect(once.checked).toBe(true);
    });
    const start = canvas.getByLabelText(
      i18n.t("schedule:start_time"),
    ) as HTMLInputElement;
    expect(start.disabled).toBe(false);
    const amount = canvas.getByLabelText(
      i18n.t("schedule:run_amount"),
    ) as HTMLInputElement;
    expect(amount.disabled).toBe(true);
  },
};

// `no_app_assigned` has neither startTime nor runInterval — "Run Continuously"
// mode. Start time and timezone are disabled because they have no effect.
export const RunContinuouslyModeReflected: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("schedule:edit_schedule", { id: 17 }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(() => {
      const cont = canvas.getByRole("radio", {
        name: i18n.t("schedule:run_continuously"),
      }) as HTMLInputElement;
      expect(cont.checked).toBe(true);
    });
    const start = canvas.getByLabelText(
      i18n.t("schedule:start_time"),
    ) as HTMLInputElement;
    expect(start.disabled).toBe(true);
    const tz = canvas.getByLabelText(i18n.t("schedule:timezone")) as HTMLSelectElement;
    expect(tz.disabled).toBe(true);
  },
};

// Editing a continuously-mode entry and selecting "Run Every" populates a
// default start time (the input is no longer empty), enables the amount and
// unit selects, and re-enables the timezone select.
export const SwitchToRunEveryEnablesControls: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("schedule:edit_schedule", { id: 17 }),
    });
    await act(async () => userEvent.click(editBtn));
    const every = await canvas.findByRole("radio", {
      name: i18n.t("schedule:run_every"),
    });
    await act(async () => userEvent.click(every));
    await waitFor(() => {
      const amount = canvas.getByLabelText(
        i18n.t("schedule:run_amount"),
      ) as HTMLInputElement;
      expect(amount.disabled).toBe(false);
    });
    const start = canvas.getByLabelText(
      i18n.t("schedule:start_time"),
    ) as HTMLInputElement;
    expect(start.disabled).toBe(false);
    expect(start.value).not.toEqual("");
    const tz = canvas.getByLabelText(i18n.t("schedule:timezone")) as HTMLSelectElement;
    expect(tz.disabled).toBe(false);
  },
};

// Picking a routing spec via the modal updates the read-only display field.
// Use the entry that already has `polltest` selected and switch it to `goes1`.
export const PickRoutingSpecViaModal: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("schedule:edit_schedule", { id: 17 }),
    });
    await act(async () => userEvent.click(editBtn));
    const chooseBtn = await canvas.findByRole("button", {
      name: i18n.t("schedule:select_routing"),
    });
    await act(async () => userEvent.click(chooseBtn));
    // Modal portals to the body — query via the global screen.
    const dialog = await screen.findByRole("dialog");
    const row = await within(dialog).findByText("goes1");
    await act(async () => userEvent.click(row));
    const select = await within(dialog).findByRole("button", {
      name: i18n.t("translation:select"),
    });
    await act(async () => userEvent.click(select));
    await waitFor(() => {
      const routing = canvas.getByLabelText(
        i18n.t("schedule:routing_spec"),
      ) as HTMLInputElement;
      expect(routing.value).toEqual("goes1");
    });
  },
};

// Saving a row posts back to the API with the new mode encoded into the
// startTime/runInterval pair. Open `goes1` in edit mode, switch to Run
// Continuously, save, and assert the POST captured both fields as null/empty
// (the Run Continuously contract) and the row collapsed back to show mode.
export const SaveContinuouslyClearsScheduleFields: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        postSchedule: http.post("/odcsapi/schedule", async ({ request }) => {
          const body = (await request.json()) as Partial<ApiScheduleEntry>;
          (
            globalThis as unknown as { __lastSchedulePost?: typeof body }
          ).__lastSchedulePost = body;
          return HttpResponse.json<ApiScheduleEntry>({});
        }),
      },
    },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("schedule:edit_schedule", { id: 9 }),
    });
    await act(async () => userEvent.click(editBtn));
    const cont = await canvas.findByRole("radio", {
      name: i18n.t("schedule:run_continuously"),
    });
    await act(async () => userEvent.click(cont));
    const saveBtn = canvas.getByRole("button", {
      name: i18n.t("schedule:save_schedule", { id: 9 }),
    });
    await act(async () => userEvent.click(saveBtn));
    // Row collapses back to show mode after the save settles.
    await waitFor(() => {
      expect(
        canvas.queryByRole("button", {
          name: i18n.t("schedule:save_schedule", { id: 9 }),
        }),
      ).not.toBeInTheDocument();
    });
    const captured = (
      globalThis as unknown as { __lastSchedulePost?: Partial<ApiScheduleEntry> }
    ).__lastSchedulePost;
    expect(captured).toBeDefined();
    expect(captured?.startTime ?? null).toBeNull();
    expect(captured?.runInterval ?? null).toBeNull();
  },
};

// Deleting a schedule fires the DELETE and the row drops out after the refetch.
export const DeleteScheduleRow: Story = {
  parameters: {
    msw: {
      handlers: (() => {
        const list = [...SCHEDULE_REFS];
        return {
          ...baseHandlers,
          scheduleRefs: http.get("/odcsapi/schedulerefs", () =>
            HttpResponse.json<ApiScheduleEntryRef[]>(list),
          ),
          deleteSchedule: http.delete("/odcsapi/schedule", ({ request }) => {
            const url = new URL(request.url);
            const id = Number(url.searchParams.get("scheduleid"));
            const idx = list.findIndex((s) => s.schedEntryId === id);
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
    expect((await canvas.findAllByText("goes2")).length).toBeGreaterThan(0);
    const deleteBtn = await canvas.findByRole("button", {
      name: i18n.t("schedule:delete_for", { id: 10 }),
    });
    await act(async () => userEvent.click(deleteBtn));
    await waitFor(() => expect(canvas.queryAllByText("goes2")).toHaveLength(0));
  },
};
