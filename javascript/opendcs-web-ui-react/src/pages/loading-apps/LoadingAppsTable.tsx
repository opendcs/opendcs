import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiAppRef, ApiLoadingApp } from "opendcs-api";
import LoadingApp, { LoadingAppSkeleton, type UiLoadingApp } from "./LoadingApp";
import type { RemoveAction, SaveAction } from "../../util/Actions";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../components/data-table";

export type TableAppRef = Partial<ApiAppRef>;

export interface LoadingAppsTableProperties {
  apps: TableAppRef[];
  getApp?: (appId: number) => Promise<ApiLoadingApp>;
  actions?: SaveAction<ApiLoadingApp> & RemoveAction<number>;
  loading?: boolean;
}

export const LoadingAppsTable: React.FC<LoadingAppsTableProperties> = ({
  apps,
  getApp,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["loadingapps", "translation"]);

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
          return isNaN(d.getTime()) ? "" : d.toLocaleString();
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
    [t, actions],
  );

  return (
    <AppDataTable<TableAppRef, number, ApiLoadingApp>
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
            : Promise.resolve({ appId: row.appId } as UiLoadingApp);
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