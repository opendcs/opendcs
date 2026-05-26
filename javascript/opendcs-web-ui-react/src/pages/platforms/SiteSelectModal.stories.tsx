import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import type { ApiSiteRef } from "opendcs-api";
import { useCallback, useState } from "react";
import { expect, fn, screen, waitFor } from "storybook/test";
import { SiteSelectModal } from "./SiteSelectModal";

const mockSiteRefs: ApiSiteRef[] = [
  {
    siteId: 100,
    publicName: "RiverPoint",
    sitenames: { "USGS-ID": "01646500" },
    description: "Public-named site.",
  },
  {
    siteId: 101,
    sitenames: { "USGS-ID": "02191300", local: "TackleBox" },
    description: "Falls back to first sitename value.",
  },
];

const handlers = {
  siteRefs: http.get("/odcsapi/siterefs", () =>
    HttpResponse.json<ApiSiteRef[]>(mockSiteRefs),
  ),
};

interface DemoProps {
  onSelect: (site: ApiSiteRef) => void;
}

const Demo: React.FC<DemoProps> = ({ onSelect }) => {
  const [show, setShow] = useState(true);
  const handleSelect = useCallback(
    (site: ApiSiteRef) => {
      onSelect(site);
      setShow(false);
    },
    [onSelect],
  );
  return (
    <SiteSelectModal
      show={show}
      onHide={() => setShow(false)}
      onSelect={handleSelect}
    />
  );
};

const meta = {
  component: Demo,
  args: { onSelect: fn() },
  parameters: { msw: { handlers } },
} satisfies Meta<typeof Demo>;

export default meta;

type Story = StoryObj<typeof meta>;

export const PicksSiteByPublicName: Story = {
  play: async ({ args, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    await screen.findByText(i18n.t("platforms:select_site"));

    // siteDisplayName picks publicName when present.
    await userEvent.click(await screen.findByText("RiverPoint"));
    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:select") }),
    );

    await waitFor(() =>
      expect(args.onSelect).toHaveBeenCalledWith(
        expect.objectContaining({ siteId: 100 }),
      ),
    );
  },
};

export const FallsBackToFirstSiteName: Story = {
  play: async ({ mount, parameters }) => {
    await mount();
    const { i18n } = parameters;
    await screen.findByText(i18n.t("platforms:select_site"));
    // When publicName is missing, siteDisplayName uses the first sitename
    // value — here "02191300".
    expect(await screen.findByText("02191300")).toBeInTheDocument();
  },
};
