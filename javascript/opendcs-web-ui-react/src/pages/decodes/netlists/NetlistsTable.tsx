import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiNetList, ApiNetlistRef } from "opendcs-api";
import Netlist, { NetlistSkeleton, type NetlistDetails } from "./Netlist";
import type { UiNetlist } from "./NetlistReducer";
import type { RemoveAction, SaveAction } from "../../../util/Actions";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";

export type TableNetlistRef = Partial<ApiNetlistRef>;

export interface NetlistsTableProperties {
  netlists: TableNetlistRef[];
  getNetlist?: (netlistId: number) => Promise<ApiNetList>;
  actions?: SaveAction<ApiNetList> & RemoveAction<number>;
  loading?: boolean;
}

export const NetlistsTable: React.FC<NetlistsTableProperties> = ({
  netlists,
  getNetlist,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["netlists", "translation"]);

  const columns = useMemo<ColumnDef<TableNetlistRef>[]>(
    () => [
      {
        data: "netlistId",
        header: t("netlists:header.Id"),
        defaultContent: "new",
        className: "dt-left",
        type: "num",
      },
      { data: "name", header: t("netlists:header.Name"), type: "string" },
      {
        data: "transportMediumType",
        header: t("netlists:header.MediumType"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "numPlatforms",
        header: t("netlists:header.NumPlatforms"),
        defaultContent: "0",
        className: "dt-center",
        type: "num",
      },
      {
        data: "lastModifyTime",
        header: t("netlists:header.LastModified"),
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
