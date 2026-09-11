import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type {
  ApiAppRef,
  ApiRoutingRef,
  ApiScheduleEntry,
  ApiScheduleEntryRef,
} from "opendcs-api";
import Schedule, { ScheduleSkeleton, type ScheduleDetails } from "./Schedule";
import type { UiSchedule } from "./ScheduleReducer";
import type { RemoveAction, SaveAction } from "../../util/Actions";
import {
  AppDataTable,
  dateColumn,
  idColumn,
  textColumn,
  type ColumnDef,
  type RowAction,
} from "../../components/data-table";

export type TableScheduleRef = Partial<ApiScheduleEntryRef>;

export interface SchedulesTableProperties {
  schedules: TableScheduleRef[];
  apps: ApiAppRef[];
  routings: ApiRoutingRef[];
  routingsLoading?: boolean;
  getSchedule?: (schedEntryId: number) => Promise<ApiScheduleEntry>;
  actions?: SaveAction<ApiScheduleEntry> & RemoveAction<number>;
  loading?: boolean;
}

export const SchedulesTable: React.FC<SchedulesTableProperties> = ({
  schedules,
  apps,
  routings,
  routingsLoading = false,
  getSchedule,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["schedule", "translation"]);

  const columns = useMemo<ColumnDef<TableScheduleRef>[]>(
    () => [
      idColumn("schedEntryId", t("schedule:header.Id")),
      {
        data: "name",
        header: t("schedule:header.Name"),
        type: "string",
        defaultSort: "asc",
      },
      textColumn("appName", t("schedule:header.LoadingApp")),
      textColumn("routingSpecName", t("schedule:header.RoutingSpec")),
      {
        data: "enabled",
        header: t("schedule:header.Enabled"),
        defaultContent: "",
        className: "dt-center",
        type: "num",
        render: (data: unknown, type: string) => {
          const enabled = Boolean(data);
          if (type !== "display") return enabled ? 1 : 0;
          return enabled ? "✓" : "";
        },
      },
      dateColumn("lastModified", t("schedule:header.LastModified")),
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TableScheduleRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("schedule:edit_schedule", { id: row.schedEntryId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        confirm: true,
        show: (row) => (row.schedEntryId ?? 0) > 0,
        aria: (row) => t("schedule:delete_for", { id: row.schedEntryId }),
        onClick: ({ row }) => {
          if (row.schedEntryId !== undefined) actions.remove?.(row.schedEntryId);
        },
      },
    ],
    [t, actions],
  );

  return (
    <AppDataTable<TableScheduleRef, number, ApiScheduleEntry>
      data={schedules}
      loading={loading}
      getId={(s) => s.schedEntryId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const detailsPromise: Promise<ScheduleDetails> =
          row.schedEntryId && row.schedEntryId > 0 && getSchedule
            ? getSchedule(row.schedEntryId).then((schedule) => ({ schedule }))
            : Promise.resolve({
                schedule: { schedEntryId: row.schedEntryId } as UiSchedule,
              });
        return (
          <Schedule
            details={detailsPromise}
            apps={apps}
            routings={routings}
            routingsLoading={routingsLoading}
            actions={{
              save: (s) => detailActions.save(s),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <ScheduleSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ schedEntryId: id, enabled: false }),
        ariaLabel: t("schedule:add_schedule"),
      }}
      onSave={actions.save}
      caption={t("schedule:title")}
      tableId="schedulesTable"
    />
  );
};

export default SchedulesTable;
