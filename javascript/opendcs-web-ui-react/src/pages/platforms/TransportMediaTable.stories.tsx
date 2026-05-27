import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import type { ApiRefList, ApiTransportMedium } from "opendcs-api";
import { expect, waitFor } from "storybook/test";
import { TransportMediaTable } from "./TransportMediaTable";

const SAMPLE: ApiTransportMedium[] = [
  {
    mediumType: "goes-self-timed",
    mediumId: "CE31D030",
    scriptName: "ST",
    channelNum: 1,
    assignedTime: 0,
    transportWindow: 10,
    transportInterval: 3600,
    timezone: "UTC",
  },
  {
    mediumType: "goes-random",
    mediumId: "CE31D031",
    scriptName: "RD",
    channelNum: 195,
  },
  {
    mediumType: "iridium",
    mediumId: "300434066009870",
    scriptName: "ST",
  },
];

// Reflists for renderDetail. The TransportMedium detail card calls
// useRefListsQuery; if a row action expands a detail row we need the API to
// respond or React Query will retry/log warnings.
const REFLISTS: Record<string, ApiRefList> = {
  TransportMediumType: {
    enumName: "TransportMediumType",
    items: {
      goes: { value: "goes", sortNumber: 1 },
      "goes-self-timed": { value: "goes-self-timed", sortNumber: 2 },
      "goes-random": { value: "goes-random", sortNumber: 3 },
      "polled-modem": { value: "polled-modem", sortNumber: 4 },
      iridium: { value: "iridium", sortNumber: 5 },
    },
  },
  LoggerType: { enumName: "LoggerType", items: {} },
  BaudRate: { enumName: "BaudRate", items: {} },
  Parity: { enumName: "Parity", items: {} },
  DataBits: { enumName: "DataBits", items: {} },
  StopBits: { enumName: "StopBits", items: {} },
};

const reflistHandler = http.get("/odcsapi/reflists", () => HttpResponse.json(REFLISTS));

const meta = {
  title: "Platforms/TransportMediaTable",
  component: TransportMediaTable,
  parameters: { msw: { handlers: [reflistHandler] } },
} satisfies Meta<typeof TransportMediaTable>;
export default meta;

type Story = StoryObj<typeof meta>;

export const Populated: Story = {
  args: { media: SAMPLE, edit: false },
  play: async ({ mount }) => {
    const canvas = await mount();
    await waitFor(() => expect(canvas.getByText("CE31D030")).toBeInTheDocument());
    expect(canvas.getByText("CE31D031")).toBeInTheDocument();
    expect(canvas.getByText("300434066009870")).toBeInTheDocument();
  },
};

export const EditMode: Story = {
  args: {
    media: SAMPLE,
    edit: true,
    actions: { save: () => undefined, remove: () => undefined },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    // Edit + delete action buttons render per row in edit mode.
    await waitFor(() =>
      expect(
        canvas.getByRole("button", {
          name: i18n.t("platforms:edit_transport", { id: "CE31D030" }),
        }),
      ).toBeInTheDocument(),
    );
    expect(
      canvas.getByRole("button", {
        name: i18n.t("platforms:delete_transport_for", { id: "CE31D030" }),
      }),
    ).toBeInTheDocument();
    // The + add button is also present.
    expect(
      canvas.getByRole("button", { name: i18n.t("platforms:add_transport") }),
    ).toBeInTheDocument();
  },
};

export const Empty: Story = {
  args: { media: [], edit: false },
};

// Deleting a row should fire actions.remove with the transportKey of the row.
// The table itself doesn't remove the row (the parent owns the data) — we just
// verify the action gets the right argument.
export const DeleteFiresRemove: Story = {
  args: {
    media: SAMPLE,
    edit: true,
  },
  play: async ({ mount, parameters, userEvent, args }) => {
    const removed: string[] = [];
    args.actions = {
      save: () => undefined,
      remove: (key) => {
        removed.push(key);
      },
    };
    const canvas = await mount();
    const { i18n } = parameters;

    const deleteBtn = await waitFor(() =>
      canvas.getByRole("button", {
        name: i18n.t("platforms:delete_transport_for", { id: "CE31D030" }),
      }),
    );
    await userEvent.click(deleteBtn);

    await waitFor(() => expect(removed).toEqual(["goes-self-timed::CE31D030"]));
  },
};
