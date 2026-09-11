import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay, http, HttpResponse } from "msw";
import type { ApiAppRef, ApiAppStatus } from "opendcs-api";
import { expect, waitFor } from "storybook/test";
import { LoadingAppsPage } from "./LoadingAppsPage";
import type { ApiLoadingApp } from "opendcs-api";

// Mock data

const mockAppRefs: ApiAppRef[] = [
  {
    appId: 1,
    appName: "compproc",
    appType: "computationprocess",
    comment: "Main computation process",
  },
  {
    appId: 2,
    appName: "routing",
    appType: "routingscheduler",
    comment: "Routing scheduler",
  },
];

// appId 1 is running; appId 2 is inactive (pid absent).
const mockAppStats: ApiAppStatus[] = [
  { appId: 1, pid: 12345, status: "Cmps: 0/0" },
  { appId: 2 },
];

const handlers = {
  appRefs: http.get("/odcsapi/apprefs", () =>
    HttpResponse.json<ApiAppRef[]>(mockAppRefs),
  ),
  appStat: http.get("/odcsapi/appstat", () =>
    HttpResponse.json<ApiAppStatus[]>(mockAppStats),
  ),
};

const meta = {
  component: LoadingAppsPage,
  parameters: { msw: { handlers } },
} satisfies Meta<typeof LoadingAppsPage>;

export default meta;

type Story = StoryObj<typeof meta>;

// Stories

// Verifies that the page loads app refs and merges running status from the
// monitor endpoint. Covers useAppStatQuery (success path), the appsWithStatus
// memo in LoadingAppsPage, and both branches of the status column renderer.
export const WithRunningAndInactiveApps: Story = {
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    await waitFor(() => canvas.getByText(i18n.t("loadingapps:status_running")), {
      timeout: 5000,
    });
    expect(canvas.getByText(i18n.t("loadingapps:status_inactive"))).toBeInTheDocument();
  },
};

// Verifies that when the monitor endpoint is unavailable, the page still
// renders with all apps shown as inactive.
export const WithUnavailableMonitor: Story = {
  parameters: {
    msw: {
      handlers: {
        ...handlers,
        appStat: http.get("/odcsapi/appstat", () => HttpResponse.error()),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    await waitFor(() => canvas.getAllByText(i18n.t("loadingapps:status_inactive")), {
      timeout: 5000,
    });
    expect(
      canvas.queryByText(i18n.t("loadingapps:status_running")),
    ).not.toBeInTheDocument();
  },
};

interface StoredApp {
  appId: number;
  appName?: string;
  comment?: string;
  manualEditingApp?: boolean;
  properties: Record<string, string>;
}

const storedApps: StoredApp[] = [
  {
    appId: 1,
    appName: "compproc",
    comment: "Main computation process",
    manualEditingApp: false,
    properties: { appType: "computationprocess" },
  },
];

const toApiApp = (app: StoredApp): ApiLoadingApp => ({
  appId: app.appId,
  appName: app.appName,
  comment: app.comment,
  manualEditingApp: app.manualEditingApp,
  appType: app.properties.appType ?? "",
  properties: { ...app.properties },
});

const statefulHandlers = {
  appRefs: http.get("/odcsapi/apprefs", async () => {
    await delay(25);
    return HttpResponse.json<ApiAppRef[]>(
      storedApps.map((app) => ({
        appId: app.appId,
        appName: app.appName,
        appType: app.properties.appType ?? "",
        comment: app.comment,
      })),
    );
  }),
  appStat: http.get("/odcsapi/appstat", async () => {
    await delay(25);
    return HttpResponse.json<ApiAppStatus[]>([]);
  }),
  getApp: http.get("/odcsapi/app", async ({ request }) => {
    await delay(25);
    const id = Number(new URL(request.url).searchParams.get("appid"));
    const app = storedApps.find((a) => a.appId === id);
    return app
      ? HttpResponse.json(toApiApp(app))
      : new HttpResponse(null, { status: 404 });
  }),
  postApp: http.post("/odcsapi/app", async ({ request }) => {
    await delay(25);
    const body = (await request.json()) as ApiLoadingApp;
    const properties: Record<string, string> = {
      ...((body.properties ?? {}) as Record<string, string>),
    };
    // The editable appType field wins over the copy in properties.
    if (body.appType) properties.appType = body.appType;
    const idx = storedApps.findIndex((a) => a.appId === body.appId);
    const saved: StoredApp = {
      appId: body.appId!,
      appName: body.appName,
      comment: body.comment,
      manualEditingApp: body.manualEditingApp,
      properties,
    };
    if (idx >= 0) storedApps[idx] = saved;
    return HttpResponse.json(toApiApp(saved), { status: 201 });
  }),
};

// Reads the live node each time: the DataTables child row is rebuilt on
// redraws, so a captured element can be detached by the next interaction.
const appNameInput = (): HTMLInputElement => {
  const el = document.querySelector("input[name='appName']");
  if (!el) throw new Error("appName input not rendered yet");
  return el as HTMLInputElement;
};

export const EditThenSaveShowsEdit: Story = {
  parameters: { msw: { handlers: statefulHandlers } },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("loadingapps:edit_app", { id: 1 }) },
      { timeout: 15000 },
    );
    await userEvent.click(editBtn);

    // DetailFade keeps the real content `visibility: hidden` until its enter
    // animation starts, and a hidden input cannot be focused or typed into.
    await waitFor(
      () => {
        expect(appNameInput().value).toBe("compproc");
        expect(document.querySelector(".detail-appear__layer--hidden")).toBeNull();
      },
      { timeout: 15000 },
    );

    await userEvent.clear(appNameInput());
    await userEvent.type(appNameInput(), "edited-name");

    const saveBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("loadingapps:save_app", { id: 1 }) },
      { timeout: 15000 },
    );
    await userEvent.click(saveBtn);

    // The server took the edit ...
    await waitFor(() => expect(storedApps[0].appName).toBe("edited-name"), {
      timeout: 5000,
    });

    // ... and it shows without a refresh, both in the reopened detail form and
    // in the table row. The row is rendered purely from the refetched app refs,
    // so it cannot be satisfied by typed text lingering in the old DOM node.
    await waitFor(() => expect(appNameInput().value).toBe("edited-name"), {
      timeout: 5000,
    });
    await waitFor(
      () => {
        const cells = [...document.querySelectorAll("td")].map((c) => c.textContent);
        expect(cells).toContain("edited-name");
      },
      { timeout: 15000 },
    );
  },
};

// The create counterpart to EditThenSaveShowsEdit. This keeps its own store
// rather than reusing `storedApps`, which EditThenSaveShowsEdit mutates — the
// assertions below need a known starting row and a known assigned id.
const NEW_APP_ID = 42;

const createStore: StoredApp[] = [];

// Captures what the editor actually POSTs, so the assertions can check the
// request body rather than only the resulting UI state.
const createdApps: ApiLoadingApp[] = [];

const resetCreateStore = () => {
  createdApps.length = 0;
  createStore.length = 0;
  createStore.push({
    appId: 1,
    appName: "compproc",
    comment: "Main computation process",
    manualEditingApp: false,
    properties: { appType: "computationprocess" },
  });
};

const createHandlers = {
  appStat: http.get("/odcsapi/appstat", async () => {
    await delay(25);
    return HttpResponse.json<ApiAppStatus[]>([]);
  }),
  appRefs: http.get("/odcsapi/apprefs", async () => {
    await delay(25);
    return HttpResponse.json<ApiAppRef[]>(
      createStore.map((app) => ({
        appId: app.appId,
        appName: app.appName,
        appType: app.properties.appType ?? "",
        comment: app.comment,
      })),
    );
  }),
  getApp: http.get("/odcsapi/app", async ({ request }) => {
    await delay(25);
    const id = Number(new URL(request.url).searchParams.get("appid"));
    const app = createStore.find((a) => a.appId === id);
    return app
      ? HttpResponse.json(toApiApp(app))
      : new HttpResponse(null, { status: 404 });
  }),
  postApp: http.post("/odcsapi/app", async ({ request }) => {
    await delay(25);
    const body = (await request.json()) as ApiLoadingApp;
    createdApps.push(body);
    // Stand in for the server assigning the id on insert.
    const saved: StoredApp = {
      appId: NEW_APP_ID,
      appName: body.appName,
      comment: body.comment,
      manualEditingApp: body.manualEditingApp,
      properties: { appType: body.appType ?? "" },
    };
    createStore.push(saved);
    return HttpResponse.json(toApiApp(saved), { status: 201 });
  }),
};

// Adding a row and saving it. The new row is held locally with a synthetic
// negative appId, which has to be normalized away so the server assigns the
// real one, and the created app has to show without a manual refresh.
export const AddThenSave: Story = {
  parameters: { msw: { handlers: createHandlers } },
  play: async ({ mount, parameters, userEvent }) => {
    resetCreateStore();
    const canvas = await mount();
    const { i18n } = parameters;

    // Let the initial refs load before adding, otherwise the redraw closes the
    // new row's child row before it can be filled in.
    await canvas.findByText("compproc");

    const addBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("loadingapps:add_app") },
      { timeout: 15000 },
    );
    await userEvent.click(addBtn);

    // DetailFade keeps the real content `visibility: hidden` until its enter
    // animation starts, and a hidden input cannot be focused or typed into.
    await waitFor(
      () => {
        expect(appNameInput()).toBeInTheDocument();
        expect(document.querySelector(".detail-appear__layer--hidden")).toBeNull();
      },
      { timeout: 15000 },
    );
    await userEvent.type(appNameInput(), "brand-new-app");

    const saveBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("loadingapps:save_app", { id: -1 }) },
      { timeout: 15000 },
    );
    await userEvent.click(saveBtn);

    // The POST has to carry the typed name and no id — sending the synthetic -1
    // would make the server treat the create as an update of a missing row.
    await waitFor(() => expect(createdApps).toHaveLength(1), { timeout: 5000 });
    expect(createdApps[0].appId).toBeUndefined();
    expect(createdApps[0].appName).toBe("brand-new-app");

    // And the created app shows without a refresh. The row is rendered purely
    // from the refetched app refs, so it cannot be satisfied by typed text
    // lingering in the old DOM node.
    await waitFor(
      () => {
        const cells = [...document.querySelectorAll("td")].map((c) => c.textContent);
        expect(cells).toContain("brand-new-app");
        expect(cells).toContain(String(NEW_APP_ID));
      },
      { timeout: 15000 },
    );
  },
};
