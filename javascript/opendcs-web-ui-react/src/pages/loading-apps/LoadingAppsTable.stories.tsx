import type { Meta, StoryObj } from "@storybook/react-vite";
import { LoadingAppsTable, type TableAppRef } from "./LoadingAppsTable";
import type { ApiAppRef, ApiLoadingApp } from "opendcs-api";
import { expect, spyOn, waitFor } from "storybook/test";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { RemoveAction, SaveAction } from "../../util/Actions";

const meta = {
  component: LoadingAppsTable,
} satisfies Meta<typeof LoadingAppsTable>;

export default meta;

type Story = StoryObj<typeof meta>;

// ---------------------------------------------------------------------------
// Shared mock data
// ---------------------------------------------------------------------------

const sharedApps: ApiLoadingApp[] = [
  {
    appId: 1,
    appName: "compproc",
    appType: "computationprocess",
    comment: "Main computation process",
    manualEditingApp: false,
    properties: {},
  },
  {
    appId: 2,
    appName: "routing",
    appType: "routingscheduler",
    comment: "Routing scheduler",
    manualEditingApp: false,
    properties: { pollingInterval: "60" },
  },
];

const toAppRefs = (apps: ApiLoadingApp[]): ApiAppRef[] =>
  apps.map(({ appId, appName, appType, comment }) => ({
    appId: appId!,
    appName,
    appType,
    comment,
  }));

const getAppFromList =
  (list: ApiLoadingApp[]) =>
  async (id: number): Promise<ApiLoadingApp> => {
    const app = list.find((a) => a.appId === id);
    if (!app) throw new Error(`No app with id ${id}`);
    return { ...app };
  };

// ---------------------------------------------------------------------------
// Stateful wrapper — owns app list so edits survive re-renders
// ---------------------------------------------------------------------------

let nextId = 100;

const LoadingAppsTableWrapper: React.FC<{ initialApps: ApiLoadingApp[] }> = ({
  initialApps,
}) => {
  const [localApps, setLocalApps] = useState<ApiLoadingApp[]>(initialApps);
  const localAppsRef = useRef(localApps);

  useEffect(() => {
    localAppsRef.current = localApps;
  }, [localApps]);

  const appRefs = useMemo(() => toAppRefs(localApps), [localApps]);

  const localGetApp = useCallback(
    (id: number) => getAppFromList(localAppsRef.current)(id),
    [],
  );

  const saveApp = useCallback((app: ApiLoadingApp) => {
    setLocalApps((prev) => {
      if ((app.appId ?? 0) < 0) {
        return [...prev, { ...app, appId: ++nextId }];
      }
      return prev.map((a) => (a.appId === app.appId ? app : a));
    });
  }, []);

  const removeApp = useCallback((appId: number) => {
    setLocalApps((prev) => prev.filter((a) => a.appId !== appId));
  }, []);

  return (
    <LoadingAppsTable
      apps={appRefs}
      getApp={localGetApp}
      actions={{ save: saveApp, remove: removeApp }}
    />
  );
};

type StoryArgs = {
  apps: TableAppRef[];
  getApp?: (appId: number) => Promise<ApiLoadingApp>;
  actions?: SaveAction<ApiLoadingApp> & RemoveAction<number>;
};

const StoryRender = (args: StoryArgs) => {
  const initialApps = sharedApps.filter((a) =>
    args.apps.some((ref) => ref.appId === a.appId),
  );
  return <LoadingAppsTableWrapper initialApps={initialApps} />;
};

// ---------------------------------------------------------------------------
// Stories
// ---------------------------------------------------------------------------

export const Default: Story = {
  args: { apps: [] },
  render: StoryRender,
  play: async ({ mount }) => {
    await mount();
  },
};

export const WithRunningStatus: Story = {
  args: {
    apps: [
      {
        appId: 1,
        appName: "compproc",
        appType: "computationprocess",
        comment: "Main computation process",
        _pid: 12345,
      },
      {
        appId: 2,
        appName: "routing",
        appType: "routingscheduler",
        comment: "Routing scheduler",
        _pid: null,
      },
    ],
  },
  play: async ({ mount }) => {
    await mount();
  },
};

export const WithApps: Story = {
  args: { apps: toAppRefs(sharedApps) },
  render: StoryRender,
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("loadingapps:edit_app", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    expect(editBtn).toBeInTheDocument();
  },
};

export const AddAppThenCancel: Story = {
  args: { apps: toAppRefs(sharedApps) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const addBtn = await canvas.findByRole("button", {
      name: i18n.t("loadingapps:add_app"),
    });
    await userEvent.click(addBtn);

    const newRow = await waitFor(() => canvas.queryByText("-1"));
    expect(newRow).toBeInTheDocument();

    const cancelBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("loadingapps:cancel_for", { id: -1 }) },
      { timeout: 5000 },
    );
    await userEvent.click(cancelBtn);

    await waitFor(() => {
      expect(canvas.queryByText("-1")).not.toBeInTheDocument();
    });
  },
};

export const EditAppSave: Story = {
  args: { apps: toAppRefs(sharedApps) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const editBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("loadingapps:edit_app", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    await userEvent.click(editBtn);

    const saveBtn = await canvas.findByRole(
      "button",
      { name: i18n.t("loadingapps:save_app", { id: 1 }) },
      { timeout: 5000 },
    );
    expect(saveBtn).toBeInTheDocument();

    await userEvent.click(saveBtn);

    const editBtnAfter = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("loadingapps:edit_app", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    expect(editBtnAfter).toBeInTheDocument();
  },
};

export const CopyAppNoHandler: Story = {
  args: { apps: toAppRefs(sharedApps) },
  render: (args) => <LoadingAppsTable apps={args.apps as TableAppRef[]} />,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const copyBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("loadingapps:copy_for", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    // No getApp handler was supplied — the click should silently no-op.
    await userEvent.click(copyBtn);
    expect(canvas.queryByText("-1")).not.toBeInTheDocument();
  },
};

export const CopyApp: Story = {
  args: { apps: toAppRefs(sharedApps) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const copyBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("loadingapps:copy_for", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    await userEvent.click(copyBtn);

    // A new draft row (-1) opens for editing, pre-filled from the source app.
    const newRow = await waitFor(() => canvas.queryByText("-1"));
    expect(newRow).toBeInTheDocument();

    const appTypeInput = await canvas.findByDisplayValue(
      "computationprocess",
      {},
      { timeout: 5000 },
    );
    expect(appTypeInput).toBeInTheDocument();
  },
};

export const CopyAppFailure: Story = {
  args: { apps: toAppRefs(sharedApps) },
  render: (args) => (
    <LoadingAppsTable
      apps={args.apps as TableAppRef[]}
      getApp={async () => Promise.reject(new Error("boom"))}
    />
  ),
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const warnSpy = spyOn(console, "warn").mockImplementation(() => {});

    const copyBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("loadingapps:copy_for", { id: 1 }),
        }),
      { timeout: 5000 },
    );
    await userEvent.click(copyBtn);

    await waitFor(() => expect(warnSpy).toHaveBeenCalled());
    expect(canvas.queryByText("-1")).not.toBeInTheDocument();

    warnSpy.mockRestore();
  },
};

export const DeleteApp: Story = {
  args: { apps: toAppRefs(sharedApps) },
  render: StoryRender,
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const deleteBtn = await waitFor(
      () =>
        canvas.getByRole("button", {
          name: i18n.t("loadingapps:delete_for", { id: 2 }),
        }),
      { timeout: 5000 },
    );
    await userEvent.click(deleteBtn);

    await waitFor(() => {
      expect(canvas.queryByText("routing")).not.toBeInTheDocument();
    });
  },
};
