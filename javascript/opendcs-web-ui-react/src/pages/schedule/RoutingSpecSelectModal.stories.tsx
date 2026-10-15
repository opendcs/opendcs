import type { Meta, StoryObj } from "@storybook/react-vite";
import { useCallback, useState } from "react";
import { delay, http, HttpResponse } from "msw";
import type { ApiRoutingRef } from "opendcs-api";
import { expect, fn, screen, waitFor } from "storybook/test";
import { RoutingSpecSelectModal } from "./RoutingSpecSelectModal";

const ROUTING_REFS: ApiRoutingRef[] = [
  {
    routingId: 101,
    name: "goes1",
    dataSourceName: "lrgs-main",
    destination: "directory",
  },
  {
    routingId: 102,
    name: "goes2",
    dataSourceName: "lrgs-main",
    destination: "directory",
  },
  {
    routingId: 103,
    name: "polltest",
    dataSourceName: "polled-modem",
    destination: "directory",
  },
];

// Wrapper that mirrors how `Schedule.tsx` drives the modal: holds `show`
// state, hides on cancel/select, and forwards the picked record to a spy.
type WrapperProps = {
  onSelect: (r: ApiRoutingRef) => void;
};

const RoutingSpecSelectModalDemo: React.FC<WrapperProps> = ({ onSelect }) => {
  const [show, setShow] = useState(true);
  const handleSelect = useCallback(
    (r: ApiRoutingRef) => {
      onSelect(r);
      setShow(false);
    },
    [onSelect],
  );
  return (
    <RoutingSpecSelectModal
      show={show}
      onHide={() => setShow(false)}
      onSelect={handleSelect}
    />
  );
};

const routingRefsReturning = (routings: ApiRoutingRef[]) => ({
  routingRefs: http.get("/odcsapi/routingrefs", () =>
    HttpResponse.json<ApiRoutingRef[]>(routings),
  ),
});

const meta = {
  component: RoutingSpecSelectModalDemo,
  args: {
    onSelect: fn(),
  },
  parameters: { msw: { handlers: routingRefsReturning(ROUTING_REFS) } },
} satisfies Meta<typeof RoutingSpecSelectModalDemo>;

export default meta;

type Story = StoryObj<typeof meta>;

// Default render: the modal opens with the routing list and its title shows.
export const Default: Story = {
  play: async ({ mount, parameters }) => {
    await mount();
    const { i18n } = parameters;
    await screen.findByText(i18n.t("schedule:select_routing"));
    expect(await screen.findByText("goes1")).toBeInTheDocument();
    expect(await screen.findByText("goes2")).toBeInTheDocument();
    expect(await screen.findByText("polltest")).toBeInTheDocument();
  },
};

// Empty state: noneMessage renders instead of the chooser table.
export const Empty: Story = {
  parameters: { msw: { handlers: routingRefsReturning([]) } },
  play: async ({ mount, parameters }) => {
    await mount();
    const { i18n } = parameters;
    expect(
      await screen.findByText(i18n.t("schedule:select_routing_none")),
    ).toBeInTheDocument();
  },
};

// Loading state: showed with no data => the spinner appears.
export const Loading: Story = {
  parameters: {
    msw: {
      handlers: {
        routingRefs: http.get("/odcsapi/routingrefs", async () => {
          await delay("infinite");
          return HttpResponse.json<ApiRoutingRef[]>([]);
        }),
      },
    },
  },
  play: async ({ mount }) => {
    await mount();
    expect(await screen.findByRole("status")).toBeInTheDocument();
  },
};

// Pick a routing spec — onSelect fires with the picked record, modal closes.
export const SelectAndConfirm: Story = {
  play: async ({ mount, args, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    await screen.findByText(i18n.t("schedule:select_routing"));
    await userEvent.click(await screen.findByText("polltest"));
    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:select") }),
    );
    await waitFor(() =>
      expect(args.onSelect).toHaveBeenCalledWith(
        expect.objectContaining({ routingId: 103, name: "polltest" }),
      ),
    );
    await waitFor(() =>
      expect(
        screen.queryByText(i18n.t("schedule:select_routing")),
      ).not.toBeInTheDocument(),
    );
  },
};

// Cancel button closes the modal without firing onSelect.
export const CancelClosesModal: Story = {
  play: async ({ mount, args, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    await screen.findByText(i18n.t("schedule:select_routing"));
    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:cancel") }),
    );
    await waitFor(() =>
      expect(
        screen.queryByText(i18n.t("schedule:select_routing")),
      ).not.toBeInTheDocument(),
    );
    expect(args.onSelect).not.toHaveBeenCalled();
  },
};
