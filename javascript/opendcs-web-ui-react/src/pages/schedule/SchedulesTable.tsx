import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type {
  ApiAppRef,
  ApiRoutingRef,
  ApiScheduleEntry,
  ApiScheduleEntryRef,
} from "opendcs-api";
import Schedule, { ScheduleSkeleton } from "./Schedule";
import type { RemoveAction, SaveAction } from "../../util/Actions";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../components/data-table";

export type TableScheduleRef = Partial<ApiScheduleEntryRef>;

export interface ScheduleTableProperties {
  schedules: TableScheduleRef[];
  apps: ApiAppRef[];
  routings: ApiRoutingRef[];
  routingsLoading?: boolean;
  getSchedule?: (schedEntryId: number) => Promise<ApiScheduleEntry>;
  actions?: SaveAction<ApiScheduleEntry> & RemoveAction<number>;
  loading?: boolean;
}

export const SchedulesTable: React.FC<ScheduleTableProperties> = ({
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
      {
        data: "schedEntryId",
        header: t("schedule:id"),
        defaultContent: "new",
        type: "num",
      },
      { data: "name", header: t("schedule:name"), type: "string" },
      { data: "appName", header: t("schedule:loading_app"), type: "string" },
      {
        data: "routingSpecName",
        header: t("schedule:routing_spec"),
        type: "string",
      },
      {
        data: "enabled",
        header: t("schedule:enabled"),
        type: "string",
        render: (_data, _type, row) => (row.enabled ? t("translation:yes") : t("translation:no")),
      },
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
        show: (row) => (row.schedEntryId ?? 0) > 0,
        aria: (row) => t("schedule:delete_for", { id: row.schedEntryId }),
        confirm: () => t("translation:confirm_delete_prompt"),
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
        const detailsPromise =
          row.schedEntryId && row.schedEntryId > 0 && getSchedule
            ? getSchedule(row.schedEntryId).then((schedule) => ({ schedule }))
            : Promise.resolve({ schedule: { schedEntryId: row.schedEntryId } });
        return (
          <Schedule
            details={detailsPromise}
            apps={apps}
            routings={routings}
            routingsLoading={routingsLoading}
            actions={{
              save: (schedule) => detailActions.save(schedule),
              cancel: () => {
                if (row.schedEntryId !== undefined) detailActions.cancel();
              },
            }}
            edit={mode !== "show"}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <ScheduleSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ schedEntryId: id }),
        ariaLabel: t("schedule:add_schedule"),
      }}
      onSave={actions.save}
      caption={t("schedule:title")}
      tableId="scheduleTable"
    />
  );
};
