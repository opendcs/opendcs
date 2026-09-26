import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type {
  ApiDataType,
  ApiTimeSeriesIdentifier,
  ApiTsGroup,
  ApiTsGroupRef,
} from "opendcs-api";
import TsGroup, { TsGroupSkeleton, type TsGroupDetails } from "./TsGroup";
import type { UiTsGroup } from "./TsGroupReducer";
import type { RemoveAction, SaveAction } from "../../../util/Actions";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";

export type TableTsGroupRef = Partial<ApiTsGroupRef>;

export interface TsGroupsTableProperties {
  groups: TableTsGroupRef[];
  getGroup?: (groupId: number) => Promise<ApiTsGroup>;
  /** Time series catalog handed down to the editor's members chooser. */
  timeSeries?: ApiTimeSeriesIdentifier[];
  timeSeriesLoading?: boolean;
  /** Data type catalog, forwarded to the editor's criteria labelling. */
  dataTypes?: ApiDataType[];
  actions?: SaveAction<ApiTsGroup> & RemoveAction<number>;
  loading?: boolean;
}

/**
 * The desktop group editor's List tab: every group in the database, expanding
 * into the full editor.
 */
export const TsGroupsTable: React.FC<TsGroupsTableProperties> = ({
  groups,
  getGroup,
  timeSeries = [],
  timeSeriesLoading = false,
  dataTypes = [],
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["tsgroups", "translation"]);

  const columns = useMemo<ColumnDef<TableTsGroupRef>[]>(
    () => [
      {
        data: "groupId",
        header: t("tsgroups:header.Id"),
        defaultContent: "new",
        className: "dt-left",
        type: "num",
      },
      { data: "groupName", header: t("tsgroups:header.Name"), type: "string" },
      {
        data: "groupType",
        header: t("tsgroups:header.Type"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "description",
        header: t("tsgroups:header.Description"),
        defaultContent: "",
        type: "string",
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TableTsGroupRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("tsgroups:edit_group", { id: row.groupId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        confirm: true,
        show: (row) => (row.groupId ?? 0) > 0,
        aria: (row) => t("tsgroups:delete_for", { id: row.groupId }),
        onClick: ({ row }) => {
          if (row.groupId !== undefined) actions.remove?.(row.groupId);
        },
      },
    ],
    [t, actions],
  );

  // Refs alone are enough to render a new group's editor; existing groups need
  // the full definition (members, sub-groups, criteria) fetched on open.
  const allGroupRefs = useMemo(
    () => groups.filter((g): g is ApiTsGroupRef => g.groupId !== undefined),
    [groups],
  );

  return (
    <AppDataTable<TableTsGroupRef, number, ApiTsGroup>
      data={groups}
      loading={loading}
      getId={(g) => g.groupId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const detailsPromise: Promise<TsGroupDetails> =
          row.groupId && row.groupId > 0 && getGroup
            ? getGroup(row.groupId).then((group) => ({ group }))
            : Promise.resolve({ group: { groupId: row.groupId } as UiTsGroup });
        return (
          <TsGroup
            details={detailsPromise}
            allGroups={allGroupRefs.filter((g) => g.groupId !== row.groupId)}
            timeSeries={timeSeries}
            timeSeriesLoading={timeSeriesLoading}
            dataTypes={dataTypes}
            actions={{
              save: (g) => detailActions.save(g),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <TsGroupSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ groupId: id }),
        ariaLabel: t("tsgroups:add_group"),
      }}
      onSave={actions.save}
      caption={t("tsgroups:title")}
      tableId="tsGroupsTable"
    />
  );
};

export default TsGroupsTable;
