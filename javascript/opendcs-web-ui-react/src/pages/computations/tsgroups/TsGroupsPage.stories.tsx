import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { http, HttpResponse } from "msw";
import type {
  ApiDataType,
  ApiInterval,
  ApiSiteRef,
  ApiTimeSeriesIdentifier,
  ApiTsGroup,
  ApiTsGroupRef,
} from "opendcs-api";
import { expect, screen, waitFor } from "storybook/test";
import { TsGroupsPage } from "./TsGroupsPage";

const GROUP_REFS: ApiTsGroupRef[] = [
  {
    groupId: 1,
    groupName: "MROI4-ROWI4-HG",
    groupType: "basin",
    description: "This is a group for the MROI4-ROWI4-HG Regression Test",
  },
  {
    groupId: 2,
    groupName: "regtest_017",
    groupType: "data-type",
    description: "Group for regression test 017",
  },
  {
    groupId: 9,
    groupName: "GateOpening",
    groupType: "basin",
    description: "For test_032 CWMS-7619",
  },
];

const FULL_GROUPS: Record<number, ApiTsGroup> = {
  1: { groupId: 1, groupName: "MROI4-ROWI4-HG", groupType: "basin", tsIds: [] },
  2: { groupId: 2, groupName: "regtest_017", groupType: "data-type", tsIds: [] },
  9: {
    groupId: 9,
    groupName: "GateOpening",
    groupType: "basin",
    description: "For test_032 CWMS-7619",
    tsIds: [
      {
        key: 1,
        uniqueString: "OKVI4.Stage.Inst.15Minutes.0.raw",
        storageUnits: "ft",
        active: true,
      },
    ],
    includeGroups: [{ groupId: 1, groupName: "MROI4-ROWI4-HG", groupType: "basin" }],
    excludeGroups: [{ groupId: 2, groupName: "regtest_017", groupType: "data-type" }],
    intersectGroups: [],
    groupAttrs: ["Param=Opening", "Version=manual-raw"],
    groupSites: [{ siteId: 2, sitenames: { CWMS: "ROWI4" }, publicName: "Iowa River" }],
    groupDataTypes: [
      { id: 224, standard: "CWMS", code: "ELEV-PZ2A", displayName: "CWMS:ELEV-PZ2A" },
    ],
  },
};

const TS_REFS: ApiTimeSeriesIdentifier[] = [
  { key: 1, uniqueString: "OKVI4.Stage.Inst.15Minutes.0.raw", storageUnits: "ft" },
  { key: 2, uniqueString: "OKVI4.Stage.Ave.1Day.1Day.CO", storageUnits: "ft" },
  { key: 3, uniqueString: "ROWI4.Flow.Inst.1Hour.0.raw", storageUnits: "cfs" },
];

const SITE_REFS: ApiSiteRef[] = [
  { siteId: 2, sitenames: { CWMS: "ROWI4" }, publicName: "IOWA RIVER NEAR ROWAN" },
  { siteId: 3, sitenames: { CWMS: "OKVI4-Spillway1" }, publicName: "Oakville" },
];

const DATA_TYPES: ApiDataType[] = [
  { id: 224, standard: "CWMS", code: "ELEV-PZ2A", displayName: "CWMS:ELEV-PZ2A" },
  { id: 225, standard: "CWMS", code: "Stage", displayName: "CWMS:Stage" },
];

const INTERVALS: ApiInterval[] = [
  { intervalId: 1, name: "1Hour" },
  { intervalId: 2, name: "15Minutes" },
];

const baseHandlers = {
  tsGroupRefs: http.get("/odcsapi/tsgrouprefs", () =>
    HttpResponse.json<ApiTsGroupRef[]>(GROUP_REFS),
  ),
  tsGroup: http.get("/odcsapi/tsgroup", ({ request }) => {
    const id = Number(new URL(request.url).searchParams.get("groupid"));
    return HttpResponse.json<ApiTsGroup>(FULL_GROUPS[id] ?? { groupId: id });
  }),
  postTsGroup: http.post("/odcsapi/tsgroup", async () =>
    HttpResponse.json<ApiTsGroup>({}),
  ),
  deleteTsGroup: http.delete("/odcsapi/tsgroup", () => HttpResponse.json({})),
  expandGroup: http.get("/odcsapi/expandgroup", () =>
    HttpResponse.json<ApiTimeSeriesIdentifier[]>(TS_REFS),
  ),
  tsRefs: http.get("/odcsapi/tsrefs", () =>
    HttpResponse.json<ApiTimeSeriesIdentifier[]>(TS_REFS),
  ),
  siteRefs: http.get("/odcsapi/siterefs", () =>
    HttpResponse.json<ApiSiteRef[]>(SITE_REFS),
  ),
  dataTypes: http.get("/odcsapi/datatypelist", () =>
    HttpResponse.json<ApiDataType[]>(DATA_TYPES),
  ),
  intervals: http.get("/odcsapi/intervals", () =>
    HttpResponse.json<ApiInterval[]>(INTERVALS),
  ),
};

const meta = {
  component: TsGroupsPage,
} satisfies Meta<typeof TsGroupsPage>;

export default meta;

type Story = StoryObj<typeof meta>;

// Default render: every group defined in the database shows in the list.
export const Default: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText("GateOpening")).toBeInTheDocument();
    expect(await canvas.findByText("regtest_017")).toBeInTheDocument();
  },
};

// Empty state: no groups defined — the caption still renders.
export const Empty: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        tsGroupRefs: http.get("/odcsapi/tsgrouprefs", () =>
          HttpResponse.json<ApiTsGroupRef[]>([]),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(await canvas.findByText(i18n.t("tsgroups:title"))).toBeInTheDocument();
  },
};

// Open a group — the full definition loads: identity fields, the explicit
// member, both sub-group lists and the criteria rows.
export const OpenGroupDetail: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await act(async () => userEvent.click(await canvas.findByText("GateOpening")));

    await waitFor(async () => {
      const nameInput = (await canvas.findByLabelText(
        i18n.t("tsgroups:groupName"),
      )) as HTMLInputElement;
      expect(nameInput.value).toEqual("GateOpening");
    });
    // Explicit member, with its TSID parts broken out into columns.
    expect(
      await canvas.findByText("OKVI4.Stage.Inst.15Minutes.0.raw"),
    ).toBeInTheDocument();
    // Sub-groups from two different combine modes share one table.
    expect(
      await canvas.findByText(i18n.t("tsgroups:subgroups.combine_exclude")),
    ).toBeInTheDocument();
    // Criteria: a site record, a data type record and two attributes.
    expect(await canvas.findByText("CWMS:ELEV-PZ2A")).toBeInTheDocument();
    expect(await canvas.findByText("manual-raw")).toBeInTheDocument();
  },
};

// Edit mode via the row action: Save appears and the name becomes editable.
export const EditMode: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("tsgroups:edit_group", { id: 9 }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(() => {
      expect(
        canvas.getByRole("button", {
          name: i18n.t("tsgroups:save_group", { id: 9 }),
        }),
      ).toBeInTheDocument();
    });
    const nameInput = canvas.getByLabelText(
      i18n.t("tsgroups:groupName"),
    ) as HTMLInputElement;
    expect(nameInput.readOnly).toBe(false);
  },
};

// Evaluate: the expanded membership of the saved definition.
export const EvaluateGroup: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    await act(async () => userEvent.click(await canvas.findByText("GateOpening")));
    const evaluateBtn = await canvas.findByRole("button", {
      name: i18n.t("tsgroups:evaluate.button_for", { name: "GateOpening" }),
    });
    await act(async () => userEvent.click(evaluateBtn));
    // The dialog is portaled to the document body, so it is queried through
    // `screen` rather than the story canvas.
    await waitFor(async () => {
      expect(
        await screen.findByText(
          i18n.t("tsgroups:evaluate.title", { name: "GateOpening" }),
        ),
      ).toBeInTheDocument();
    });
    expect(await screen.findByText("ROWI4.Flow.Inst.1Hour.0.raw")).toBeInTheDocument();
  },
};

// The criteria panel's Location dialog, replicating the desktop's full / base
// / sub choice: picking a site and scoping it to "base" stores a BaseLocation
// attribute rather than the site record itself.
export const AddBaseLocationCriterion: Story = {
  parameters: { msw: { handlers: baseHandlers } },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("tsgroups:edit_group", { id: 9 }),
    });
    await act(async () => userEvent.click(editBtn));

    const locationLabel = i18n.t("tsgroups:criteria.part.Location");
    const addLocationBtn = await canvas.findByRole("button", {
      name: i18n.t("tsgroups:criteria.add_title", { part: locationLabel }),
    });
    await act(async () => userEvent.click(addLocationBtn));

    // Dialogs are portaled to the document body, so they are queried through
    // `screen` rather than the story canvas.
    const siteRow = await screen.findByText("OKVI4-Spillway1");
    await act(async () => userEvent.click(siteRow));

    const baseRadio = await screen.findByLabelText(
      i18n.t("tsgroups:criteria.scope_base", { part: locationLabel }),
    );
    await act(async () => userEvent.click(baseRadio));

    const resultInput = (await screen.findByLabelText(
      i18n.t("tsgroups:criteria.result"),
    )) as HTMLInputElement;
    await waitFor(() => expect(resultInput.value).toEqual("OKVI4"));

    await act(async () =>
      userEvent.click(
        await screen.findByRole("button", {
          name: i18n.t("tsgroups:criteria.add_confirm"),
        }),
      ),
    );

    await waitFor(async () => {
      expect(
        await canvas.findByText(i18n.t("tsgroups:criteria.part.BaseLocation")),
      ).toBeInTheDocument();
    });
  },
};
