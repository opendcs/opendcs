import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { http, HttpResponse } from "msw";
import type { ApiNetList, ApiNetlistRef } from "opendcs-api";
import { expect, screen, waitFor } from "storybook/test";
import { NetlistsPage } from "./NetlistsPage";

const NETLIST_REFS: ApiNetlistRef[] = [
  {
    netlistId: 1,
    name: "BFD-BMD",
    transportMediumType: "goes",
    siteNameTypePref: "nwshb5",
    numPlatforms: 3,
    lastModifyTime: new Date("2020-08-22T14:36:55.705Z"),
  },
  {
    netlistId: 4,
    name: "USGS-Sites",
    transportMediumType: "other",
    siteNameTypePref: "nwshb5",
    numPlatforms: 2,
    lastModifyTime: new Date("2020-10-19T18:14:14.788Z"),
  },
];

const FULL_NETLISTS: Record<number, ApiNetList> = {
  1: {
    netlistId: 1,
    name: "BFD-BMD",
    transportMediumType: "goes",
    siteNameTypePref: "nwshb5",
    lastModifyTime: new Date("2020-08-22T14:36:55.705Z"),
    items: {
      BFDBMD01: {
        transportId: "BFDBMD01",
        platformName: "BFD",
        description: "Buford Dam",
      },
    },
  },
  4: {
    netlistId: 4,
    name: "USGS-Sites",
    transportMediumType: "other",
    siteNameTypePref: "nwshb5",
    lastModifyTime: new Date("2020-10-19T18:14:14.788Z"),
    items: {
      "14159500": {
        transportId: "14159500",
        platformName: "CGRO",
        description: "",
      },
      "14372300": {
        transportId: "14372300",
        platformName: "AGNO",
        description: "",
      },
    },
  },
};

const baseHandlers = {
  netlistRefs: http.get("/odcsapi/netlistrefs", () =>
    HttpResponse.json<ApiNetlistRef[]>(NETLIST_REFS),
  ),
  netlist: http.get("/odcsapi/netlist", ({ request }) => {
    const url = new URL(request.url);
    const id = Number(url.searchParams.get("netlistid"));
    return HttpResponse.json<ApiNetList>(FULL_NETLISTS[id] ?? { netlistId: id });
  }),
  postNetlist: http.post("/odcsapi/netlist", async () =>
    HttpResponse.json<ApiNetList>({}),
  ),
  deleteNetlist: http.delete("/odcsapi/netlist", () => HttpResponse.json({})),
};

const meta = {
  component: NetlistsPage,
} satisfies Meta<typeof NetlistsPage>;

export default meta;

type Story = StoryObj<typeof meta>;

// Default render: list comes back, all netlists show.
export const Default: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText("BFD-BMD")).toBeInTheDocument();
    expect(await canvas.findByText("USGS-Sites")).toBeInTheDocument();
  },
};

// Empty state: API returns nothing — caption still renders.
export const Empty: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        netlistRefs: http.get("/odcsapi/netlistrefs", () =>
          HttpResponse.json<ApiNetlistRef[]>([]),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(await canvas.findByText(i18n.t("netlists:title"))).toBeInTheDocument();
  },
};

// Open a netlist row — detail loads, name prefilled.
export const OpenNetlistDetail: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await act(async () => userEvent.click(await canvas.findByText("BFD-BMD")));
    await waitFor(async () => {
      const nameInput = (await canvas.findByLabelText(
        i18n.t("netlists:name"),
      )) as HTMLInputElement;
      expect(nameInput.value).toEqual("BFD-BMD");
    });
  },
};

// Open in edit mode via the row edit-action; the Save button appears and the
// name field becomes editable.
export const EditMode: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("netlists:edit_netlist", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(() => {
      expect(
        canvas.getByRole("button", {
          name: i18n.t("netlists:save_netlist", { id: 1 }),
        }),
      ).toBeInTheDocument();
    });
    const nameInput = canvas.getByLabelText(
      i18n.t("netlists:name"),
    ) as HTMLInputElement;
    expect(nameInput.readOnly).toBe(false);
  },
};

// Open in edit mode then cancel — the row closes and the cancel button goes away.
export const EditAndCancel: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("netlists:edit_netlist", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));
    const cancelBtn = await canvas.findByRole("button", {
      name: i18n.t("netlists:cancel_for", { id: 1 }),
    });
    await act(async () => userEvent.click(cancelBtn));
    await waitFor(() => {
      expect(
        canvas.queryByRole("button", {
          name: i18n.t("netlists:cancel_for", { id: 1 }),
        }),
      ).not.toBeInTheDocument();
    });
  },
};

// The "+" header button appends a new editable netlist row.
export const AddNewNetlistRow: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("netlists:add_netlist"),
    });
    await act(async () => userEvent.click(addBtn));
    await waitFor(() => {
      const nameInput = canvas.getByLabelText(
        i18n.t("netlists:name"),
      ) as HTMLInputElement;
      expect(nameInput.value).toEqual("");
      expect(nameInput.readOnly).toBe(false);
    });
  },
};

// Deleting a netlist fires the DELETE and the row drops out after the refetch.
export const DeleteNetlistRow: Story = {
  parameters: {
    msw: {
      handlers: (() => {
        const list = [...NETLIST_REFS];
        return {
          ...baseHandlers,
          netlistRefs: http.get("/odcsapi/netlistrefs", () =>
            HttpResponse.json<ApiNetlistRef[]>(list),
          ),
          deleteNetlist: http.delete("/odcsapi/netlist", ({ request }) => {
            const url = new URL(request.url);
            const id = Number(url.searchParams.get("netlistid"));
            const idx = list.findIndex((n) => n.netlistId === id);
            if (idx >= 0) list.splice(idx, 1);
            return HttpResponse.json({});
          }),
        };
      })(),
    },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(await canvas.findByText("USGS-Sites")).toBeInTheDocument();
    const deleteBtn = await canvas.findByRole("button", {
      name: i18n.t("netlists:delete_for", { id: 4 }),
    });
    await act(async () => userEvent.click(deleteBtn));
    const confirmBtn = await screen.findByRole("button", { name: "Delete" });
    await act(async () => userEvent.click(confirmBtn));
    await waitFor(() =>
      expect(canvas.queryByText("USGS-Sites")).not.toBeInTheDocument(),
    );
  },
};
