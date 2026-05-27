import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import type { ApiRefList, ApiTransportMedium } from "opendcs-api";
import { expect, waitFor, within } from "storybook/test";
import { TransportMedium } from "./TransportMedium";

const REFLISTS: Record<string, ApiRefList> = {
  TransportMediumType: {
    enumName: "TransportMediumType",
    defaultValue: "goes",
    items: {
      goes: { value: "goes", description: "GOES DCP", sortNumber: 1 },
      "goes-self-timed": {
        value: "goes-self-timed",
        description: "GOES Self-Timed",
        sortNumber: 2,
      },
      "goes-random": {
        value: "goes-random",
        description: "GOES Random",
        sortNumber: 3,
      },
      "polled-modem": {
        value: "polled-modem",
        description: "Polled via modem",
        sortNumber: 4,
      },
      "polled-tcp": {
        value: "polled-tcp",
        description: "Polled via TCP",
        sortNumber: 5,
      },
      "incoming-tcp": {
        value: "incoming-tcp",
        description: "Incoming TCP",
        sortNumber: 6,
      },
      "data-logger": {
        value: "data-logger",
        description: "Data Logger File",
        sortNumber: 7,
      },
      iridium: { value: "iridium", description: "Iridium IMEI", sortNumber: 8 },
      other: { value: "other", description: "Other", sortNumber: 9 },
    },
  },
  LoggerType: {
    enumName: "LoggerType",
    items: {
      Sutron: { value: "Sutron", sortNumber: 1 },
      Campbell: { value: "Campbell", sortNumber: 2 },
    },
  },
  BaudRate: {
    enumName: "BaudRate",
    items: {
      "300": { value: "300", sortNumber: 1 },
      "1200": { value: "1200", sortNumber: 2 },
      "9600": { value: "9600", sortNumber: 5 },
    },
  },
  Parity: {
    enumName: "Parity",
    items: {
      none: { value: "none", sortNumber: 1 },
      even: { value: "even", sortNumber: 2 },
      odd: { value: "odd", sortNumber: 3 },
    },
  },
  DataBits: {
    enumName: "DataBits",
    items: {
      "7": { value: "7", sortNumber: 1 },
      "8": { value: "8", sortNumber: 2 },
    },
  },
  StopBits: {
    enumName: "StopBits",
    items: {
      "1": { value: "1", sortNumber: 1 },
      "2": { value: "2", sortNumber: 2 },
    },
  },
};

const reflistHandler = http.get("/odcsapi/reflists", () => HttpResponse.json(REFLISTS));

const goesMedium: ApiTransportMedium = {
  mediumType: "goes",
  mediumId: "CE31D030",
  scriptName: "ST",
  channelNum: 195,
  assignedTime: 0,
  transportWindow: 10,
  transportInterval: 3600,
  timezone: "UTC",
};

const modemMedium: ApiTransportMedium = {
  mediumType: "polled-modem",
  mediumId: "+1-555-0123",
  scriptName: "MODEM",
  loggerType: "Sutron",
  baud: 9600,
  parity: "none",
  dataBits: 8,
  stopBits: 1,
  doLogin: true,
  username: "stationA",
  password: "secret",
};

const tcpMedium: ApiTransportMedium = {
  mediumType: "polled-tcp",
  mediumId: "192.168.1.1:9999",
  scriptName: "TCP",
  loggerType: "Campbell",
  doLogin: false,
};

const iridiumMedium: ApiTransportMedium = {
  mediumType: "iridium",
  mediumId: "300434066009870",
  scriptName: "IRI",
};

const meta = {
  title: "Platforms/TransportMedium",
  component: TransportMedium,
  parameters: {
    msw: { handlers: [reflistHandler] },
  },
} satisfies Meta<typeof TransportMedium>;
export default meta;

type Story = StoryObj<typeof meta>;

// View mode just renders read-only fields. Quick smoke test.
export const GoesView: Story = {
  args: { medium: goesMedium, edit: false, originalKey: "goes::CE31D030" },
};

// Edit-mode GOES should expose channelNum / assignedTime / transportWindow /
// transportInterval; serial + login fields must stay hidden.
export const GoesEdit: Story = {
  args: { medium: goesMedium, edit: true, originalKey: "goes::CE31D030" },
  play: async ({ mount }) => {
    const canvas = await mount();
    await waitFor(() => expect(canvas.getByLabelText(/channel/i)).toBeInTheDocument(), {
      timeout: 5000,
    });
    expect(canvas.getByLabelText(/assigned time/i)).toBeInTheDocument();
    expect(canvas.getByLabelText(/window/i)).toBeInTheDocument();
    // Serial + login fields must NOT be in the DOM for GOES.
    expect(canvas.queryByLabelText(/baud/i)).not.toBeInTheDocument();
    expect(canvas.queryByLabelText(/parity/i)).not.toBeInTheDocument();
    expect(canvas.queryByLabelText(/do login/i)).not.toBeInTheDocument();
  },
};

// Edit-mode polled-modem should expose serial + login fields and (because
// doLogin is true) the username and password inputs.
export const PolledModemEdit: Story = {
  args: { medium: modemMedium, edit: true, originalKey: "polled-modem::+1-555-0123" },
  play: async ({ mount }) => {
    const canvas = await mount();
    await waitFor(() => expect(canvas.getByLabelText(/baud/i)).toBeInTheDocument(), {
      timeout: 5000,
    });
    expect(canvas.getByLabelText(/parity/i)).toBeInTheDocument();
    expect(canvas.getByLabelText(/data bits/i)).toBeInTheDocument();
    expect(canvas.getByLabelText(/stop bits/i)).toBeInTheDocument();
    expect(canvas.getByLabelText(/logger type/i)).toBeInTheDocument();
    expect(canvas.getByLabelText(/do login/i)).toBeInTheDocument();
    expect(canvas.getByLabelText(/username/i)).toBeInTheDocument();
    expect(canvas.getByLabelText(/password/i)).toBeInTheDocument();
    // GOES-specific fields are hidden under polled-modem.
    expect(canvas.queryByLabelText(/channel/i)).not.toBeInTheDocument();
    expect(canvas.queryByLabelText(/assigned time/i)).not.toBeInTheDocument();
  },
};

// Edit-mode polled-tcp should expose login fields but NOT serial fields.
export const PolledTcpEdit: Story = {
  args: { medium: tcpMedium, edit: true, originalKey: "polled-tcp::192.168.1.1:9999" },
  play: async ({ mount }) => {
    const canvas = await mount();
    await waitFor(
      () => expect(canvas.getByLabelText(/logger type/i)).toBeInTheDocument(),
      { timeout: 5000 },
    );
    expect(canvas.getByLabelText(/do login/i)).toBeInTheDocument();
    expect(canvas.queryByLabelText(/baud/i)).not.toBeInTheDocument();
    expect(canvas.queryByLabelText(/parity/i)).not.toBeInTheDocument();
    // doLogin is false on this fixture, so username + password should be hidden.
    expect(canvas.queryByLabelText(/username/i)).not.toBeInTheDocument();
    expect(canvas.queryByLabelText(/password/i)).not.toBeInTheDocument();
  },
};

// Iridium / shef / other show only the common five fields.
export const IridiumEdit: Story = {
  args: { medium: iridiumMedium, edit: true, originalKey: "iridium::300434066009870" },
  play: async ({ mount }) => {
    const canvas = await mount();
    await waitFor(
      () => expect(canvas.getByLabelText(/medium type/i)).toBeInTheDocument(),
      { timeout: 5000 },
    );
    expect(canvas.queryByLabelText(/channel/i)).not.toBeInTheDocument();
    expect(canvas.queryByLabelText(/baud/i)).not.toBeInTheDocument();
    expect(canvas.queryByLabelText(/logger type/i)).not.toBeInTheDocument();
    expect(canvas.queryByLabelText(/do login/i)).not.toBeInTheDocument();
  },
};

// A→B→A field restoration. Type a channelNum, switch to polled-modem, switch
// back, and the channelNum value must come back.
export const TypeSwitchRestoresFields: Story = {
  args: { medium: goesMedium, edit: true, originalKey: "goes::CE31D030" },
  play: async ({ mount, userEvent }) => {
    const canvas = await mount();
    const typeSelect = await waitFor(
      () => canvas.getByLabelText(/medium type/i) as HTMLSelectElement,
      { timeout: 5000 },
    );

    // Reflists load async via tanstack-query; wait for the polled-modem
    // option before attempting to select it.
    await waitFor(
      () =>
        expect(
          within(typeSelect).getByRole("option", { name: /polled-modem/ }),
        ).toBeInTheDocument(),
      { timeout: 5000 },
    );

    // Switch GOES → polled-modem; serial fields must now be visible.
    await userEvent.selectOptions(typeSelect, "polled-modem");
    await waitFor(() => expect(canvas.getByLabelText(/baud/i)).toBeInTheDocument());
    expect(canvas.queryByLabelText(/channel/i)).not.toBeInTheDocument();

    // Switch polled-modem → GOES; channelNum must be restored to 195.
    await userEvent.selectOptions(typeSelect, "goes");
    const channel = (await waitFor(() =>
      canvas.getByLabelText(/channel/i),
    )) as HTMLInputElement;
    expect(channel.value).toBe("195");
  },
};

// Toggling doLogin off should hide the username + password rows.
export const DoLoginTogglesCredentials: Story = {
  args: { medium: modemMedium, edit: true, originalKey: "polled-modem::+1-555-0123" },
  play: async ({ mount, userEvent }) => {
    const canvas = await mount();
    const username = await waitFor(() => canvas.getByLabelText(/username/i), {
      timeout: 5000,
    });
    expect(username).toBeInTheDocument();

    const doLogin = canvas.getByLabelText(/do login/i) as HTMLInputElement;
    await userEvent.click(doLogin);

    await waitFor(() => {
      expect(canvas.queryByLabelText(/username/i)).not.toBeInTheDocument();
      expect(canvas.queryByLabelText(/password/i)).not.toBeInTheDocument();
    });
  },
};
