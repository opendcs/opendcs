import type { Meta, StoryObj } from "@storybook/react-vite";

import { PlatformsPage } from "./PlatformsPage";
import { http, HttpResponse } from "msw";
import {
  ApiPlatform,
  ApiPlatformConfig,
  ApiPlatformRef,
  ApiSite,
} from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { act } from "react";
import { expect, waitFor } from "storybook/test";
import { Route, Routes } from "react-router-dom";

// Shared fixture set used by the page-level stories. Tests pick subsets via
// the MSW handlers below.
const SITES: ApiSite[] = [
  {
    siteId: 11,
    sitenames: { cwms: "Alder Springs" },
    publicName: "Alder Springs",
    elevation: 5,
    elevUnits: "ft",
  },
  {
    siteId: 22,
    sitenames: { cwms: "Beta Site" },
    publicName: "Beta Site",
  },
];

const CONFIGS: ApiPlatformConfig[] = [
  { configId: 101, name: "Standard CFG", description: "Default config" },
  { configId: 202, name: "Backup CFG", description: "Backup config" },
];

const PLATFORM_REFS: ApiPlatformRef[] = [
  {
    platformId: 1,
    name: "Alpha",
    agency: "USGS",
    siteId: 11,
    configId: 101,
    config: "Standard CFG",
    description: "Alpha platform",
    designator: "A",
    transportMedia: { GOES: "ABCD1234" },
  },
  {
    platformId: 2,
    name: "Bravo",
    agency: "NOAA",
    siteId: 22,
    configId: 202,
    config: "Backup CFG",
    description: "Bravo platform",
    designator: "B",
  },
];

const fullPlatform = (ref: ApiPlatformRef): ApiPlatform => ({
  platformId: ref.platformId,
  name: ref.name,
  agency: ref.agency,
  siteId: ref.siteId,
  configId: ref.configId,
  description: ref.description,
  designator: ref.designator,
  production: false,
  properties: { lookup: "value" },
});

const baseHandlers = {
  platformRefs: http.get("/odcsapi/platformrefs", () =>
    HttpResponse.json<ApiPlatformRef[]>(PLATFORM_REFS),
  ),
  platform: http.get("/odcsapi/platform", ({ request }) => {
    const url = new URL(request.url);
    const id = Number(url.searchParams.get("platformid"));
    const ref = PLATFORM_REFS.find((p) => p.platformId === id);
    if (!ref) return HttpResponse.json({}, { status: 404 });
    return HttpResponse.json<ApiPlatform>(fullPlatform(ref));
  }),
  postPlatform: http.post("/odcsapi/platform", async () =>
    HttpResponse.json<ApiPlatform>({}),
  ),
  deletePlatform: http.delete("/odcsapi/platform", () => HttpResponse.json({})),
  site: http.get("/odcsapi/site", ({ request }) => {
    const url = new URL(request.url);
    const id = Number(url.searchParams.get("siteid"));
    const site = SITES.find((s) => s.siteId === id);
    return HttpResponse.json<ApiSite>(site ?? {});
  }),
  config: http.get("/odcsapi/config", ({ request }) => {
    const url = new URL(request.url);
    const id = Number(url.searchParams.get("configid"));
    const cfg = CONFIGS.find((c) => c.configId === id);
    return HttpResponse.json<ApiPlatformConfig>(cfg ?? {});
  }),
};

const meta = {
  component: PlatformsPage,
  decorators: [
    (Story) => (
      <Routes>
        <Route path="/platforms" element={<Story />} />
        <Route
          path="/sites"
          element={<div data-testid="sites-page-stub">Sites stub</div>}
        />
        <Route path="*" element={<Story />} />
      </Routes>
    ),
  ],
} satisfies Meta<typeof PlatformsPage>;

export default meta;

type Story = StoryObj<typeof meta>;

// 1. Default render: list comes back, both platforms show up in the table.
export const Default: Story = {
  parameters: {
    msw: { handlers: baseHandlers },
  },
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText("Alpha")).toBeInTheDocument();
    expect(await canvas.findByText("Bravo")).toBeInTheDocument();
  },
};

// 2. Empty state: the API returns nothing — table still renders.
export const Empty: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        platformRefs: http.get("/odcsapi/platformrefs", () =>
          HttpResponse.json<ApiPlatformRef[]>([]),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(
      await canvas.findByText(i18n.t("platforms:platformsTitle")),
    ).toBeInTheDocument();
  },
};

// 3. Click a row, detail card loads and shows the resolved site name.
export const OpenPlatformDetail: Story = {
  parameters: {
    msw: { handlers: baseHandlers },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const alphaRow = await canvas.findByText("Alpha");
    await act(async () => userEvent.click(alphaRow));
    // The siteId field is populated from the resolved site fetch.
    await waitFor(async () => {
      const siteInput = await canvas.findByLabelText(i18n.t("platforms:public_name"));
      expect((siteInput as HTMLInputElement).value).toEqual("Alder Springs");
    });
  },
};

// 4. Open in edit mode (via row edit-action button), then cancel — the row
//    closes and the cancel handler is invoked.
export const EditAndCancel: Story = {
  parameters: {
    msw: { handlers: baseHandlers },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("platforms:edit_platform", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));
    const cancelBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("platforms:cancel_for", { id: 1 }) },
      { timeout: 5000 },
    );
    await act(async () => userEvent.click(cancelBtn));
    await waitFor(() => {
      expect(
        canvas.queryByRole("button", {
          name: i18n.t("platforms:cancel_for", { id: 1 }),
        }),
      ).not.toBeInTheDocument();
    });
  },
};

// 5. Open in edit mode, click the "edit site" button — verifies the page
//    navigates to /sites (the route stub appears).
export const EditSiteNavigatesToSites: Story = {
  parameters: {
    msw: { handlers: baseHandlers },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("platforms:edit_platform", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));
    // Wait for the detail's fade-in to finish before querying for buttons
    // inside it — on slower CI machines the Suspense + DetailFade sequence
    // can exceed the default 1 s asyncUtilTimeout that waitFor shares with
    // findByRole, so an explicit longer timeout is required here.
    await waitFor(
      () => {
        expect(
          canvas.getByRole("button", {
            name: i18n.t("platforms:save_platform", { id: 1 }),
          }),
        ).toBeInTheDocument();
      },
      { timeout: 5000 },
    );
    const editSiteBtn = await canvas.findByRole("button", {
      name: i18n.t("platforms:edit_site"),
    });
    await act(async () => userEvent.click(editSiteBtn));
    await waitFor(() => {
      expect(canvas.queryByTestId("sites-page-stub")).toBeInTheDocument();
    });
  },
};
