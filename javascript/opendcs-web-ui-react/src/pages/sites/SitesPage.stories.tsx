import type { Meta, StoryObj } from "@storybook/react-vite";

import { SitesPage } from "./SitesPage";
import { http, HttpResponse } from "msw";
import {
  ApiSite,
  ApiSiteRef,
} from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { act } from "react";
import { expect } from "storybook/test";

const meta = {
  component: SitesPage,
} satisfies Meta<typeof SitesPage>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
  parameters: {
    msw: {
      handlers: {
        sites: http.get("/odcsapi/siterefs", () => {
          return HttpResponse.json<ApiSiteRef[]>([
            { siteId: 1, sitenames: { cwms: "Alder Springs" } },
            { siteId: 2, sitenames: { cwms: "test" } },
          ]);
        }),
        site: http.get("/odcsapi/site", ({ request }) => {
          const url = new URL(request.url);
          return HttpResponse.json<ApiSite>({
            siteId: parseInt(url.searchParams.get("siteId") || "-2"),
            sitenames: { cwms: "Alder Springs" },
            elevation: 5,
            elevUnits: "ft",
            publicName: "Alder Springs",
            active: true,
          });
        }),
      },
    },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const alderSpringsRow = await canvas.findByText("Alder Springs");
    await act(async () => userEvent.click(alderSpringsRow));

    const elevationInput = await canvas.findByLabelText(i18n.t("elevation"));
    expect((elevationInput as HTMLInputElement).value).toEqual("5");
  },
};

const sharedMswHandlers = {
  sites: http.get("/odcsapi/siterefs", () => {
    return HttpResponse.json<ApiSiteRef[]>([
      { siteId: 1, sitenames: { cwms: "Alder Springs" } },
      { siteId: 2, sitenames: { cwms: "test" } },
    ]);
  }),
  site: http.get("/odcsapi/site", ({ request }) => {
    const url = new URL(request.url);
    return HttpResponse.json<ApiSite>({
      siteId: parseInt(url.searchParams.get("siteId") || "-2"),
      sitenames: { cwms: "Alder Springs" },
      elevation: 5,
      elevUnits: "ft",
      publicName: "Alder Springs",
      active: true,
    });
  }),
  saveSite: http.post("/odcsapi/site", () => {
    return HttpResponse.json<ApiSite>({
      siteId: 1,
      sitenames: { cwms: "Alder Springs" },
      elevation: 5,
      elevUnits: "ft",
      publicName: "Alder Springs",
      active: true,
    });
  }),
};

// Covers SitesPage.tsx: the `save: (site) => saveSite.mutateAsync(site)` arrow
// function body (line 44) and the full useSaveSiteMutation onSuccess path for
// an existing site (siteId > 0) — removeQueries + invalidateList.
export const SaveExistingSite: Story = {
  args: {},
  parameters: { msw: { handlers: sharedMswHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const { i18n } = parameters;
    const canvas = await mount();

    const editButton = await canvas.findByRole("button", {
      name: i18n.t("sites:edit_site", { id: 1 }),
    });
    await act(async () => userEvent.click(editButton));

    const saveButton = await canvas.findByRole(
      "button",
      { name: i18n.t("sites:save_site", { id: 1 }) },
      { timeout: 5000 },
    );
    await act(async () => userEvent.click(saveButton));
  },
};

// Covers the onSuccess branch where siteId <= 0 (new site, no server id yet)
// so removeQueries is skipped and only invalidateList fires.
export const SaveNewSite: Story = {
  args: {},
  parameters: { msw: { handlers: sharedMswHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const { i18n } = parameters;
    const canvas = await mount();

    const addButton = await canvas.findByRole("button", {
      name: i18n.t("sites:add_site"),
    });
    await act(async () => userEvent.click(addButton));

    const saveButton = await canvas.findByRole("button", {
      name: i18n.t("sites:save_site", { id: -1 }),
    });
    await act(async () => userEvent.click(saveButton));
  },
};
