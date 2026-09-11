import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiNetList, ApiNetlistRef, ApiPlatformRef } from "opendcs-api";
import Netlist, { NetlistSkeleton, type NetlistDetails } from "./Netlist";
import type { UiNetlist } from "./NetlistReducer";
import type { RemoveAction, SaveAction } from "../../../util/Actions";
import {
  AppDataTable,
  dateColumn,
  idColumn,
  textColumn,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";

export type TableNetlistRef = Partial<ApiNetlistRef>;

export interface NetlistsTableProperties {
  netlists: TableNetlistRef[];
  /** All platforms, for selecting netlist items. */
  platforms?: ApiPlatformRef[];
  platformsLoading?: boolean;
  getNetlist?: (netlistId: number) => Promise<ApiNetList>;
  actions?: SaveAction<ApiNetList> & RemoveAction<number>;
  loading?: boolean;
}

export const NetlistsTable: React.FC<NetlistsTableProperties> = ({
  netlists,
  platforms,
  platformsLoading = false,
  getNetlist,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["netlists", "translation"]);

  const columns = useMemo<ColumnDef<TableNetlistRef>[]>(
    () => [
      idColumn("netlistId", t("netlists:header.Id")),
      {
        data: "name",
        header: t("netlists:header.Name"),
        type: "string",
        defaultSort: "asc",
      },
      textColumn("transportMediumType", t("netlists:header.MediumType")),
      {
        data: "numPlatforms",
        header: t("netlists:header.NumPlatforms"),
        defaultContent: "0",
        className: "dt-center",
        type: "num",
      },
      dateColumn("lastModifyTime", t("netlists:header.LastModified")),
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TableNetlistRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("netlists:edit_netlist", { id: row.netlistId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        confirm: true,
        show: (row) => (row.netlistId ?? 0) > 0,
        aria: (row) => t("netlists:delete_for", { id: row.netlistId }),
        onClick: ({ row }) => {
          if (row.netlistId !== undefined) actions.remove?.(row.netlistId);
        },
      },
    ],
    [t, actions],
  );

  return (
    <AppDataTable<TableNetlistRef, number, ApiNetList>
      data={netlists}
      loading={loading}
      getId={(n) => n.netlistId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const detailsPromise: Promise<NetlistDetails> =
          row.netlistId && row.netlistId > 0 && getNetlist
            ? getNetlist(row.netlistId).then((netlist) => ({ netlist }))
            : Promise.resolve({
                netlist: { netlistId: row.netlistId } as UiNetlist,
              });
        return (
          <Netlist
            details={detailsPromise}
            actions={{
              save: (n) => detailActions.save(n),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
            platforms={platforms}
            platformsLoading={platformsLoading}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <NetlistSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ netlistId: id }),
        ariaLabel: t("netlists:add_netlist"),
      }}
      onSave={actions.save}
      caption={t("netlists:title")}
      tableId="netlistsTable"
    />
  );
};

export default NetlistsTable;
