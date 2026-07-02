import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type {
  ApiSite,
  ApiSiteRef,
} from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import Site, { SiteSkeleton, type UiSite } from "./Site";
import type { RemoveAction, SaveAction } from "../../util/Actions";
import {
  AppDataTable,
  type AppDataTableHandle,
  type ColumnDef,
  type RowAction,
} from "../../components/data-table";

export type TableSiteRef = Partial<ApiSiteRef>;

export interface SiteTableProperties {
  sites: TableSiteRef[];
  getSite?: (siteId: number) => Promise<ApiSite>;
  actions?: SaveAction<ApiSite> & RemoveAction<number>;
  loading?: boolean;
  tableRef?: React.Ref<AppDataTableHandle>;
}

export const SitesTable: React.FC<SiteTableProperties> = ({
  sites,
  getSite,
  actions = {},
  loading = false,
  tableRef,
}) => {
  const [t] = useTranslation(["sites", "translation"]);

  const columns = useMemo<ColumnDef<TableSiteRef>[]>(
    () => [
      {
        data: "siteId",
        header: t("sites:site_id"),
        defaultContent: "new",
        type: "num",
      },
      {
        data: null,
        header: t("sites:site_name"),
        render: (_data, _type, row) =>
          row.sitenames
            ? Object.entries(row.sitenames)
                .map(([k, v]) => `${k}: ${v}`)
                .join(", ")
            : "",
      },
      { data: "publicName", header: t("sites:public_name"), type: "string" },
      { data: "description", header: t("sites:description"), type: "string" },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TableSiteRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("sites:edit_site", { id: row.siteId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        show: (row) => (row.siteId ?? 0) > 0,
        aria: (row) => t("sites:delete_for", { id: row.siteId }),
        confirm: () => t("translation:confirm_delete_prompt"),
        onClick: ({ row }) => {
          if (row.siteId !== undefined) actions.remove?.(row.siteId);
        },
      },
    ],
    [t, actions],
  );

  return (
    <AppDataTable<TableSiteRef, number, ApiSite>
      data={sites}
      loading={loading}
      getId={(s) => s.siteId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const sitePromise: Promise<UiSite> =
          row.siteId && row.siteId > 0 && getSite
            ? getSite(row.siteId)
            : Promise.resolve({ siteId: row.siteId } as UiSite);
        return (
          <Site
            site={sitePromise}
            actions={{
              save: (site) => detailActions.save(site),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <SiteSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ siteId: id }),
        ariaLabel: t("sites:add_site"),
      }}
      onSave={actions.save}
      caption={t("sites:title")}
      tableId="siteTable"
      ref={tableRef}
    />
  );
};
