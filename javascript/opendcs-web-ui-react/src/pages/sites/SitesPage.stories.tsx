import type { Meta, StoryObj } from "@storybook/react-vite";

import { SitesPage } from "./SitesPage";
import { http, HttpResponse } from "msw";
import {
  ApiSite,
  ApiSiteRef,
} from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { act } from "react";
import { waitFor } from "@testing-library/dom";
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
            { siteId: 1, sitenames: { CWMS: "Alder Springs" } },
            { siteId: 2, sitenames: { CWMS: "test" } },
          ]);
        }),
        site: http.get("/odcsapi/site", ({ request }) => {
          const url = new URL(request.url);
          return HttpResponse.json<ApiSite>({
            siteId: parseInt(url.searchParams.get("siteId") || "-2"),
            sitenames: { CWMS: "Alder Springs" },
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
    //await waitFor(async () => );
  },
};
