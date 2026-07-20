import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiConfigSensor } from "opendcs-api";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";
import type { RemoveAction } from "../../../util/Actions";
import ConfigSensor, { ConfigSensorSkeleton } from "./ConfigSensor";

function joinDataTypes(dt?: { [k: string]: string }): string {
  if (!dt) return "";
  return Object.entries(dt)
    .map(([std, code]) => `${std}:${code}`)
    .join(", ");
}

export interface ConfigSensorsTableProperties {
  sensors: ApiConfigSensor[];
  actions?: {
    save?: (sensor: ApiConfigSensor, originalSensorNumber?: number) => void;
  } & RemoveAction<number>;
  /** Edit mode of the parent Config detail card. When false, row actions are hidden. */
  edit?: boolean;
}

export const ConfigSensorsTable: React.FC<ConfigSensorsTableProperties> = ({
  sensors,
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["configs", "translation"]);

  const columns = useMemo<ColumnDef<ApiConfigSensor>[]>(
    () => [
      {
        data: "sensorNumber",
        header: t("configs:sensor_num"),
        defaultContent: "",
        type: "num",
      },
      {
        data: "sensorName",
        header: t("configs:sensor_name"),
        defaultContent: "",
        type: "string",
      },
      {
        data: null,
        header: t("configs:data_types"),
        defaultContent: "",
        type: "string",
        render: (_d, _ty, row) => joinDataTypes(row.dataTypes),
      },
      {
        data: "recordingMode",
        header: t("configs:recording_mode"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "recordingInterval",
        header: t("configs:recording_interval"),
        defaultContent: "",
        type: "num",
      },
      {
        data: "absoluteMin",
        header: t("configs:absolute_min"),
        defaultContent: "",
        type: "num",
      },
      {
        data: "absoluteMax",
        header: t("configs:absolute_max"),
        defaultContent: "",
        type: "num",
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<ApiConfigSensor>[]>(
    () =>
      edit
        ? [
            {
              key: "edit",
              icon: "bi-pencil",
              variant: "warning",
              aria: (row) => t("configs:edit_sensor", { id: row.sensorNumber }),
              onClick: ({ row, api }) => api.setMode(row, "edit"),
            },
            {
              key: "delete",
              icon: "bi-trash",
              variant: "danger",
              show: (row) => row.sensorNumber !== undefined,
              aria: (row) => t("configs:delete_sensor_for", { id: row.sensorNumber }),
              onClick: ({ row }) => {
                if (row.sensorNumber !== undefined) actions.remove?.(row.sensorNumber);
              },
            },
          ]
        : [],
    [edit, t, actions],
  );

  return (
    <AppDataTable<ApiConfigSensor, number, ApiConfigSensor>
      data={sensors}
      getId={(s) => s.sensorNumber ?? -1}
      columns={columns}
      actionsLabel={edit ? t("translation:actions") : undefined}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => (
        <ConfigSensor
          sensor={row}
          edit={mode !== "show"}
          otherSensorNumbers={sensors
            .filter((s) => s.sensorNumber !== row.sensorNumber)
            .map((s) => s.sensorNumber)
            .filter((n): n is number => n !== undefined)}
          actions={{
            save: (sensor) => {
              actions.save?.(sensor, row.sensorNumber);
              detailActions.save(sensor);
            },
            cancel: () => detailActions.cancel(),
          }}
        />
      )}
      renderSkeleton={({ mode }) => (
        <ConfigSensorSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={
        edit
          ? {
              template: () => ({}),
              ariaLabel: t("configs:add_sensor"),
            }
          : undefined
      }
      caption={t("configs:config_sensors")}
      tableId="configSensorsTable"
      dataTableOptions={{ stateSave: false }}
    />
  );
};

export default ConfigSensorsTable;
