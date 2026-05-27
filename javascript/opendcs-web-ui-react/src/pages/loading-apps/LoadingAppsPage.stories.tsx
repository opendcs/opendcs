import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import type { ApiAppRef, ApiAppStatus } from "opendcs-api";
import { expect, waitFor } from "storybook/test";
import { LoadingAppsPage } from "./LoadingAppsPage";

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
