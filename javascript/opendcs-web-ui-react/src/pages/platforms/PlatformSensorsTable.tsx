import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiConfigSensor, ApiPlatformSensor } from "opendcs-api";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../components/data-table";
import type { RemoveAction } from "../../util/Actions";
import PlatformSensor, { PlatformSensorSkeleton } from "./PlatformSensor";

/**
 * A row in the sensors table: every sensor defined on the *config* lines up
 * with an optional per-platform override (matched by sensor number). The
 * config is the source of truth for which sensors exist; the platform only
 * carries differences (actual site, narrower limits, USGS DDNO, properties).
 */
export interface CombinedSensorRow {
  sensorNumber: number;
  configSensor: ApiConfigSensor;
  platformSensor?: ApiPlatformSensor;
}

function buildRows(
  configSensors: ApiConfigSensor[],
  platformSensors: ApiPlatformSensor[],
): CombinedSensorRow[] {
  const byNum = new Map<number, ApiPlatformSensor>();
  for (const ps of platformSensors) {
    if (ps.sensorNum !== undefined) byNum.set(ps.sensorNum, ps);
  }
  return configSensors
    .filter((cs) => cs.sensorNumber !== undefined)
    .map((cs) => ({
      sensorNumber: cs.sensorNumber!,
      configSensor: cs,
      platformSensor: byNum.get(cs.sensorNumber!),
    }));
}

function joinDataTypes(dt?: { [k: string]: string }): string {
  if (!dt) return "";
  return Object.entries(dt)
    .map(([std, code]) => `${std}:${code}`)
    .join(", ");
}

export interface PlatformSensorsTableProperties {
  configSensors: ApiConfigSensor[];
  platformSensors: ApiPlatformSensor[];
  actions?: {
    save?: (sensor: ApiPlatformSensor, originalSensorNum?: number) => void;
  } & RemoveAction<number>;
  /** Edit-mode of the parent Platform detail card. When false, row actions are hidden. */
  edit?: boolean;
}

export const PlatformSensorsTable: React.FC<PlatformSensorsTableProperties> = ({
  configSensors,
  platformSensors,
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["platforms", "translation"]);

  const rows = useMemo(
    () => buildRows(configSensors, platformSensors),
    [configSensors, platformSensors],
  );

  const columns = useMemo<ColumnDef<CombinedSensorRow>[]>(
    () => [
      {
        data: null,
        header: t("platforms:sensor_num"),
        defaultContent: "",
        type: "num",
        render: (_d, _ty, row) => row.sensorNumber,
      },
      {
        data: null,
        header: t("platforms:sensor_name"),
        defaultContent: "",
        type: "string",
        render: (_d, _ty, row) => row.configSensor.sensorName ?? "",
      },
      {
        data: null,
        header: t("platforms:data_types"),
        defaultContent: "",
        type: "string",
        render: (_d, _ty, row) => joinDataTypes(row.configSensor.dataTypes),
      },
      {
        data: null,
        header: t("platforms:sensor_min"),
        defaultContent: "",
        type: "num",
        render: (_d, _ty, row) =>
          row.platformSensor?.min ?? row.configSensor.absoluteMin ?? "",
      },
      {
        data: null,
        header: t("platforms:sensor_max"),
        defaultContent: "",
        type: "num",
        render: (_d, _ty, row) =>
          row.platformSensor?.max ?? row.configSensor.absoluteMax ?? "",
      },
      {
        data: null,
        header: t("platforms:actual_site_id"),
        defaultContent: "",
        type: "num",
        render: (_d, _ty, row) => row.platformSensor?.actualSiteId ?? "",
      },
      {
        data: null,
        header: t("platforms:usgs_ddno"),
        defaultContent: "",
        type: "num",
        render: (_d, _ty, row) => row.platformSensor?.usgsDdno ?? "",
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<CombinedSensorRow>[]>(
    () =>
      edit
        ? [
            {
              key: "edit",
              icon: "bi-pencil",
              variant: "warning",
              aria: (row) => t("platforms:edit_sensor", { id: row.sensorNumber }),
              onClick: ({ row, api }) => api.setMode(row, "edit"),
            },
            {
              key: "clear-override",
              icon: "bi-trash",
              variant: "danger",
              show: (row) => row.platformSensor !== undefined,
              aria: (row) => t("platforms:delete_sensor_for", { id: row.sensorNumber }),
              onClick: ({ row }) => {
                actions.remove?.(row.sensorNumber);
              },
            },
          ]
        : [],
    [edit, t, actions],
  );

  return (
    <AppDataTable<CombinedSensorRow, number, ApiPlatformSensor>
      data={rows}
      getId={(r) => r.sensorNumber}
      columns={columns}
      actionsLabel={edit ? t("translation:actions") : undefined}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => (
        <PlatformSensor
          configSensor={row.configSensor}
          override={row.platformSensor ?? { sensorNum: row.sensorNumber }}
          edit={mode !== "show"}
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
        <PlatformSensorSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      caption={t("platforms:platform_sensors")}
      tableId="platformSensorsTable"
      dataTableOptions={{ stateSave: false }}
    />
  );
};

export default PlatformSensorsTable;
