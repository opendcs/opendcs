import type { Decorator, Meta, StoryObj } from "@storybook/react-vite";
import { act, useEffect, useState, type ReactNode } from "react";
import { http, HttpResponse } from "msw";
import type {
  ApiNetList,
  ApiNetlistRef,
  ApiPlatformRef,
  ApiRefList,
  User,
} from "opendcs-api";
import { expect, fn, screen, waitFor, within } from "storybook/test";
import { BasicUser } from "../../../../.storybook/mock/TestUsers";
import { AuthContext } from "../../../contexts/app/AuthContext";
import { RefListProvider } from "../../../contexts/data/RefListProvider";
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

// BFD-goes is already on BFD-BMD and Ice-Station has no GOES medium, so only
// ALLG1-goes is offered when selecting platforms for that GOES list.
const PLATFORM_REFS: ApiPlatformRef[] = [
  {
    platformId: 10,
    name: "BFD-goes",
    agency: "CWMS",
    config: "BFD-CFG",
    description: "Buford Dam",
    transportMedia: { "goes-self": "BFDBMD01" },
    sitenames: { NWSHB5: "BFD" },
  },
  {
    platformId: 11,
    name: "ALLG1-goes",
    agency: "CWMS",
    config: "ALLG1-CFG",
    description: "Allatoona Dam",
    transportMedia: { "goes-self": "CE31D030" },
    sitenames: { NWSHB5: "ALLG1" },
  },
  {
    platformId: 12,
    name: "Ice-Station",
    transportMedia: { iridium: "300234010000000" },
  },
];

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
  platformRefs: http.get("/odcsapi/platformrefs", () =>
    HttpResponse.json<ApiPlatformRef[]>(PLATFORM_REFS),
  ),
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

// "Select platforms" offers only platforms with a medium of the list's type
// that aren't already on it, and adds the chosen ones with their transport id,
// preferred site name and description filled in (issue #2029).
export const SelectPlatformsAddsItems: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("netlists:edit_netlist", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));
    const selectBtn = await canvas.findByRole("button", {
      name: i18n.t("netlists:items.select_platforms"),
    });
    await act(async () => userEvent.click(selectBtn));

    const rowCheck = await screen.findByRole("checkbox", {
      name: i18n.t("netlists:items.select_platform", { name: "ALLG1-goes" }),
    });
    expect(screen.queryByText("Ice-Station")).not.toBeInTheDocument();
    expect(screen.queryByText("BFD-goes")).not.toBeInTheDocument();

    await act(async () => userEvent.click(rowCheck));
    const confirmBtn = await screen.findByRole("button", {
      name: i18n.t("netlists:items.add_selected", { count: 1 }),
    });
    await act(async () => userEvent.click(confirmBtn));

    await waitFor(() => {
      expect(canvas.getByText("CE31D030")).toBeInTheDocument();
      expect(canvas.getByText("ALLG1")).toBeInTheDocument();
      expect(canvas.getByText("Allatoona Dam")).toBeInTheDocument();
    });
  },
};

// Items are only added through "Select platforms" (no manual add row), and
// editing an item only exposes its description.
export const ItemsAddedOnlyBySelectingPlatforms: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters, canvasElement }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("netlists:edit_netlist", { id: 1 }),
    });
    await act(async () => userEvent.click(editBtn));
    expect(
      await canvas.findByRole("button", {
        name: i18n.t("netlists:items.select_platforms"),
      }),
    ).toBeInTheDocument();
    expect(
      canvas.queryByRole("button", { name: i18n.t("translation:add") }),
    ).not.toBeInTheDocument();

    const editItemBtn = await canvas.findByRole("button", {
      name: i18n.t("netlists:items.edit", { transportId: "BFDBMD01" }),
    });
    await act(async () => userEvent.click(editItemBtn));
    const descInput = await canvas.findByLabelText(
      i18n.t("netlists:items.description_input", { name: "BFDBMD01" }),
    );
    expect(
      canvasElement.querySelector('#netlistItemsTable input[name="platformName"]'),
    ).toBeNull();
    expect(
      canvasElement.querySelector('#netlistItemsTable input[name="transportId"]'),
    ).toBeNull();

    await act(async () => userEvent.clear(descInput));
    await act(async () => userEvent.type(descInput, "Buford headwater"));
    await act(async () =>
      userEvent.click(
        canvas.getByRole("button", {
          name: i18n.t("netlists:items.save_edit", { transportId: "BFDBMD01" }),
        }),
      ),
    );
    await waitFor(() => {
      expect(canvas.getByText("Buford headwater")).toBeInTheDocument();
      expect(canvas.getByText("BFD")).toBeInTheDocument();
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
    const confirmBtn = await screen.findByRole("button", {
      name: i18n.t("translation:delete"),
    });
    await act(async () => userEvent.click(confirmBtn));
    await waitFor(() =>
      expect(canvas.queryByText("USGS-Sites")).not.toBeInTheDocument(),
    );
  },
};

const REF_LISTS: Record<string, ApiRefList> = {
  TransportMediumType: {
    enumName: "TransportMediumType",
    items: { goes: { value: "goes" }, iridium: { value: "iridium" } },
  },
  SiteNameType: {
    enumName: "SiteNameType",
    items: { local: { value: "local" }, nwshb5: { value: "nwshb5" } },
  },
};

// The app's own provider in place of the always-ready storybook mock, so the
// form sees /reflists the way it does in the running app.
const withLiveRefLists: Decorator = (Story) => (
  <AuthContext
    value={{
      user: BasicUser,
      isLoading: false,
      loginSchemes: {},
      setUser: fn(),
      setSchemes: fn(),
      logout: fn(),
    }}
  >
    <RefListProvider>
      <Story />
    </RefListProvider>
  </AuthContext>
);

// "+Add" clicked before /reflists has answered: the selects fill in once it
// does, and the new netlist saves with the chosen types (issue #2200).
let releaseRefLists: () => void = () => {};
const lateRefListPosts: ApiNetList[] = [];
export const AddNewNetlistBeforeRefListsLoad: Story = {
  decorators: [withLiveRefLists],
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        refLists: http.get("/odcsapi/reflists", async () => {
          await new Promise<void>((resolve) => {
            releaseRefLists = resolve;
          });
          return HttpResponse.json(REF_LISTS);
        }),
        postNetlist: http.post("/odcsapi/netlist", async ({ request }) => {
          lateRefListPosts.push((await request.json()) as ApiNetList);
          return HttpResponse.json<ApiNetList>({});
        }),
      },
    },
  },
  play: async ({ mount, userEvent, parameters }) => {
    lateRefListPosts.length = 0;
    const canvas = await mount();
    const { i18n } = parameters;
    // Looked up inside waitFor: run on its own, the namespace may not be loaded yet.
    const addBtn = await waitFor(() =>
      canvas.getByRole("button", { name: i18n.t("netlists:add_netlist") }),
    );
    await act(async () => userEvent.click(addBtn));
    const nameInput = await canvas.findByRole("textbox", {
      name: i18n.t("netlists:name"),
    });
    await act(async () => userEvent.type(nameInput, "New-List"));

    await act(async () => releaseRefLists());
    const mediumSelect = await canvas.findByRole("combobox", {
      name: i18n.t("netlists:transportMediumType"),
    });
    await waitFor(() =>
      expect(within(mediumSelect).getByRole("option", { name: "goes" })).toBeEnabled(),
    );
    await act(async () => userEvent.selectOptions(mediumSelect, "goes"));
    const siteNameSelect = await canvas.findByRole("combobox", {
      name: i18n.t("netlists:siteNameTypePref"),
    });
    await act(async () => userEvent.selectOptions(siteNameSelect, "nwshb5"));
    const saveBtn = await canvas.findByRole("button", {
      name: new RegExp(`^${i18n.t("netlists:save_netlist", { id: "" }).trim()}`),
    });
    await act(async () => userEvent.click(saveBtn));
    await waitFor(() => expect(lateRefListPosts).toHaveLength(1));
    expect(lateRefListPosts[0]).toMatchObject({
      name: "New-List",
      transportMediumType: "goes",
      siteNameTypePref: "nwshb5",
    });
  },
};

// Signed out at first, like the app is while the login page shows.
let signIn: () => void = () => {};
const SignInLater = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | undefined>(undefined);
  useEffect(() => {
    signIn = () => setUser(BasicUser);
  }, []);
  return (
    <AuthContext
      value={{
        user,
        isLoading: false,
        loginSchemes: {},
        setUser: fn(),
        setSchemes: fn(),
        logout: fn(),
      }}
    >
      <RefListProvider>{children}</RefListProvider>
    </AuthContext>
  );
};

// /reflists rejects anyone without a session, so it is only asked for once the
// user is signed in. Asked earlier, the rejection stuck and the selects of a
// new netlist had nothing to choose from (issue #2200).
let signedIn = false;
let refListRequests = 0;
export const AddNewNetlistAfterSignIn: Story = {
  decorators: [
    (Story) => (
      <SignInLater>
        <Story />
      </SignInLater>
    ),
  ],
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        refLists: http.get("/odcsapi/reflists", () => {
          refListRequests += 1;
          return signedIn
            ? HttpResponse.json(REF_LISTS)
            : HttpResponse.json({ message: "Unauthorized" }, { status: 401 });
        }),
      },
    },
  },
  play: async ({ mount, userEvent, parameters }) => {
    signedIn = false;
    refListRequests = 0;
    const canvas = await mount();
    const { i18n } = parameters;
    expect(await canvas.findByText("BFD-BMD")).toBeInTheDocument();
    expect(refListRequests).toBe(0);

    signedIn = true;
    await act(async () => signIn());
    const addBtn = await waitFor(() =>
      canvas.getByRole("button", { name: i18n.t("netlists:add_netlist") }),
    );
    await act(async () => userEvent.click(addBtn));
    const mediumSelect = await canvas.findByRole("combobox", {
      name: i18n.t("netlists:transportMediumType"),
    });
    expect(within(mediumSelect).getByRole("option", { name: "goes" })).toBeEnabled();
    const siteNameSelect = await canvas.findByRole("combobox", {
      name: i18n.t("netlists:siteNameTypePref"),
    });
    expect(
      within(siteNameSelect).getByRole("option", { name: "nwshb5" }),
    ).toBeEnabled();
  },
};

// When the lists really can't be had, the selects say so instead of opening
// onto nothing (issue #2200).
export const AddNewNetlistRefListsUnavailable: Story = {
  decorators: [withLiveRefLists],
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        refLists: http.get("/odcsapi/reflists", () =>
          HttpResponse.json({ message: "boom" }, { status: 500 }),
        ),
      },
    },
  },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const addBtn = await waitFor(() =>
      canvas.getByRole("button", { name: i18n.t("netlists:add_netlist") }),
    );
    await act(async () => userEvent.click(addBtn));
    const unavailable = i18n.t("translation:reference_lists_unavailable");
    const mediumSelect = await canvas.findByRole("combobox", {
      name: `${i18n.t("netlists:transportMediumType")} (${unavailable})`,
    });
    expect(mediumSelect).toBeDisabled();
    expect(mediumSelect).toHaveTextContent(i18n.t("translation:unavailable_short"));
  },
};
