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
  appRefs: http.get("/api/apprefs", () => HttpResponse.json<ApiAppRef[]>(mockAppRefs)),
  appStat: http.get("/api/appstat", () =>
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
        appStat: http.get("/api/appstat", () => HttpResponse.error()),
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
  appRefs: http.get("/api/apprefs", async () => {
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
  appStat: http.get("/api/appstat", async () => {
    await delay(25);
    return HttpResponse.json<ApiAppStatus[]>([]);
  }),
  getApp: http.get("/api/app", async ({ request }) => {
    await delay(25);
    const id = Number(new URL(request.url).searchParams.get("appid"));
    const app = storedApps.find((a) => a.appId === id);
    return app
      ? HttpResponse.json(toApiApp(app))
      : new HttpResponse(null, { status: 404 });
  }),
  postApp: http.post("/api/app", async ({ request }) => {
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
