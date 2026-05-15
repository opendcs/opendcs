import type { Meta, StoryObj } from "@storybook/react-vite";
import { LoadingAppsPage } from "./LoadingAppsPage";
import { http, HttpResponse } from "msw";
import type { ApiAppRef, ApiLoadingApp } from "opendcs-api";
import { act } from "react";
import { expect } from "storybook/test";

const meta = {
  component: LoadingAppsPage,
} satisfies Meta<typeof LoadingAppsPage>;

export default meta;

type Story = StoryObj<typeof meta>;

const mockRefs: ApiAppRef[] = [
  {
    appId: 1,
    appName: "compproc",
    appType: "computationprocess",
    comment: "Computation process",
  },
  {
    appId: 2,
    appName: "dbimport",
    appType: "utility",
    comment: "Database import utility",
  },
];

const mockApps: ApiLoadingApp[] = [
  {
    appId: 1,
    appName: "compproc",
    appType: "computationprocess",
    comment: "Computation process",
    manualEditingApp: false,
    properties: { OfficeID: "CWMS", logFile: "compproc.log" },
  },
  {
    appId: 2,
    appName: "dbimport",
    appType: "utility",
    comment: "Database import utility",
    manualEditingApp: true,
    properties: {},
  },
];

export const Default: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: {
        apprefs: http.get("/odcsapi/apprefs", () =>
          HttpResponse.json<ApiAppRef[]>(mockRefs),
        ),
        app: http.get("/odcsapi/app", ({ request }) => {
          const url = new URL(request.url);
          const id = parseInt(url.searchParams.get("appid") ?? "-1");
          return HttpResponse.json<ApiLoadingApp>(
            mockApps.find((a) => a.appId === id) ?? mockApps[0],
          );
        }),
      },
    },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const compProcRow = await canvas.findByText("compproc");
    await act(async () => userEvent.click(compProcRow));

    const appNameInput = await canvas.findByLabelText(i18n.t("loadingapps:app_name"));
    expect((appNameInput as HTMLInputElement).value).toEqual("compproc");
  },
};

export const EditSave: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: {
        apprefs: http.get("/odcsapi/apprefs", () =>
          HttpResponse.json<ApiAppRef[]>(mockRefs),
        ),
        app: http.get("/odcsapi/app", ({ request }) => {
          const url = new URL(request.url);
          const id = parseInt(url.searchParams.get("appid") ?? "-1");
          return HttpResponse.json<ApiLoadingApp>(
            mockApps.find((a) => a.appId === id) ?? mockApps[0],
          );
        }),
        save: http.post("/odcsapi/app", async ({ request }) => {
          const body = (await request.json()) as ApiLoadingApp;
          return HttpResponse.json<ApiLoadingApp>({ ...body, appId: body.appId ?? 99 });
        }),
      },
    },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await canvas.findByLabelText(
      i18n.t("loadingapps:edit_app", { id: 1 }),
    );
    await act(async () => userEvent.click(editBtn));

    const saveBtn = await canvas.findByLabelText(
      i18n.t("loadingapps:save_app", { id: 1 }),
    );
    expect(saveBtn).toBeTruthy();
  },
};
