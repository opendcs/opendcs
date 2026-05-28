import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type {
  ApiNetlistRef,
  ApiPlatformRef,
  ApiRouting,
  ApiRoutingRef,
} from "opendcs-api";
import Routing, { RoutingSkeleton, type RoutingDetails } from "./Routing";
import type { UiRouting } from "./RoutingReducer";
import type { RemoveAction, SaveAction } from "../../../util/Actions";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";

export type TableRoutingRef = Partial<ApiRoutingRef>;

export interface RoutingsTableProperties {
  routings: TableRoutingRef[];
  platforms: ApiPlatformRef[];
  platformsLoading?: boolean;
  netlists: ApiNetlistRef[];
  netlistsLoading?: boolean;
  getRouting?: (routingId: number) => Promise<ApiRouting>;
  actions?: SaveAction<ApiRouting> & RemoveAction<number>;
  loading?: boolean;
}

export const RoutingsTable: React.FC<RoutingsTableProperties> = ({
  routings,
  platforms,
  platformsLoading = false,
  netlists,
  netlistsLoading = false,
  getRouting,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["routing", "translation"]);

  const columns = useMemo<ColumnDef<TableRoutingRef>[]>(
    () => [
      {
        data: "routingId",
        header: t("routing:header.Id"),
        defaultContent: "new",
        className: "dt-left",
        type: "num",
      },
      { data: "name", header: t("routing:header.Name"), type: "string" },
      {
        data: "dataSourceName",
        header: t("routing:header.DataSource"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "destination",
        header: t("routing:header.Consumer"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "lastModified",
        header: t("routing:header.LastModified"),
        defaultContent: "",
        type: "date",
        render: (data: unknown, type: string) => {
          if (type !== "display") return data;
          if (!data) return "";
          const d = data instanceof Date ? data : new Date(data as string);
          return Number.isNaN(d.getTime()) ? "" : d.toLocaleString();
        },
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TableRoutingRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("routing:edit_routing", { id: row.routingId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        show: (row) => (row.routingId ?? 0) > 0,
        aria: (row) => t("routing:delete_for", { id: row.routingId }),
        onClick: ({ row }) => {
          if (row.routingId !== undefined) actions.remove?.(row.routingId);
        },
      },
    ],
    [t, actions],
  );

  return (
    <AppDataTable<TableRoutingRef, number, ApiRouting>
      data={routings}
      loading={loading}
      getId={(r) => r.routingId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const detailsPromise: Promise<RoutingDetails> =
          row.routingId && row.routingId > 0 && getRouting
            ? getRouting(row.routingId).then((routing) => ({ routing }))
            : Promise.resolve({
                routing: { routingId: row.routingId } as UiRouting,
              });
        return (
          <Routing
            details={detailsPromise}
            platforms={platforms}
            platformsLoading={platformsLoading}
            netlists={netlists}
            netlistsLoading={netlistsLoading}
            actions={{
              save: (r) => detailActions.save(r),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <RoutingSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ routingId: id }),
        ariaLabel: t("routing:add_routing"),
      }}
      onSave={actions.save}
      caption={t("routing:title")}
      tableId="routingsTable"
    />
  );
};

export default RoutingsTable;
