import { useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import type { ApiAppRef, ApiLoadingApp } from "opendcs-api";
import LoadingApp, { LoadingAppSkeleton, type UiLoadingApp } from "./LoadingApp";
import type { RemoveAction, SaveAction } from "../../util/Actions";
import {
  AppDataTable,
  type AppDataTableHandle,
  type ColumnDef,
  type RowAction,
} from "../../components/data-table";

// DataTables sees a row-data change when status loads, triggering a redraw.
export type TableAppRef = Partial<ApiAppRef> & {
  _pid?: number | null;
  _status?: string | null;
};

export interface LoadingAppsTableProperties {
  apps: TableAppRef[];
  getApp?: (appId: number) => Promise<ApiLoadingApp>;
  actions?: SaveAction<ApiLoadingApp> & RemoveAction<number>;
  loading?: boolean;
}

const toTableRef = (app: UiLoadingApp): TableAppRef => ({
  appId: app.appId,
  appName: app.appName,
  appType: app.appType,
  comment: app.comment,
});

const copiedApp = (source: ApiLoadingApp, newId: number): UiLoadingApp => ({
  ...source,
  appId: newId,
  appName: "",
  lastModified: undefined,
});

export const LoadingAppsTable: React.FC<LoadingAppsTableProperties> = ({
  apps,
  getApp,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["loadingapps", "translation"]);
  const tableRef = useRef<AppDataTableHandle<TableAppRef>>(null);
  const draftsRef = useRef<Record<number, UiLoadingApp>>({});

  const columns = useMemo<ColumnDef<TableAppRef>[]>(
    () => [
      {
        data: "appId",
        header: t("loadingapps:app_id"),
        defaultContent: "new",
        type: "num",
      },
      { data: "appName", header: t("loadingapps:app_name"), type: "string" },
      { data: "appType", header: t("loadingapps:app_type"), type: "string" },
      { data: "comment", header: t("loadingapps:comment"), type: "string" },
      {
        data: "lastModified",
        header: t("loadingapps:last_modified"),
        type: "date",
        render: (data: unknown, type: string) => {
          if (type !== "display") return data;
          if (!data) return "";
          const d = data instanceof Date ? data : new Date(data as string);
          return Number.isNaN(d.getTime()) ? "" : d.toLocaleString();
        },
      },
      {
        data: "_pid",
        header: t("loadingapps:status"),
        orderable: false,
        searchable: false,
        render: (data: unknown, type: string, row: TableAppRef) => {
          if (type !== "display") return data ?? "";
          const running = data != null;
          const label = running
            ? t("loadingapps:status_running")
            : t("loadingapps:status_inactive");
          const cls = running ? "bg-success" : "bg-secondary";
          const detail = running && row._status ? ` — ${row._status}` : "";
          return `<span class="badge ${cls}">${label}</span>${detail}`;
        },
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TableAppRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("loadingapps:edit_app", { id: row.appId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "copy",
        icon: "bi-files",
        variant: "info",
        show: (row) => (row.appId ?? 0) > 0,
        aria: (row) => t("loadingapps:copy_for", { id: row.appId }),
        onClick: async ({ row }) => {
          if (!getApp || !row.appId) return;
          try {
            const source = await getApp(row.appId);
            tableRef.current?.appendLocalItem((newId) => {
              const draft = copiedApp(source, newId);
              draftsRef.current[newId] = draft;
              return toTableRef(draft);
            }, "new");
          } catch (err) {
            console.warn(`Failed to copy loading app ${row.appId}`, err);
          }
        },
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        show: (row) => (row.appId ?? 0) > 0,
        aria: (row) => t("loadingapps:delete_for", { id: row.appId }),
        onClick: ({ row }) => {
          if (row.appId !== undefined) actions.remove?.(row.appId);
        },
      },
    ],
    [t, getApp, actions],
  );

  return (
    <AppDataTable<TableAppRef, number, ApiLoadingApp>
      ref={tableRef}
      data={apps}
      loading={loading}
      getId={(a) => a.appId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const appPromise: Promise<UiLoadingApp> =
          row.appId && row.appId > 0 && getApp
            ? getApp(row.appId)
            : Promise.resolve(
                draftsRef.current[row.appId ?? 0] ??
                  ({ appId: row.appId } as UiLoadingApp),
              );
        return (
          <LoadingApp
            app={appPromise}
            actions={{
              save: (app) => detailActions.save(app),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <LoadingAppSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ appId: id }),
        ariaLabel: t("loadingapps:add_app"),
      }}
      onSave={actions.save}
      caption={t("loadingapps:title")}
      tableId="loadingAppsTable"
    />
  );
};
